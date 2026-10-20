package com.chunyu.pregnancy.admin;

import static org.assertj.core.api.Assertions.*;

import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.common.util.RevisionHeaders;
import com.chunyu.pregnancy.admin.config.*;
import com.chunyu.pregnancy.admin.security.AdminTokenService;
import com.chunyu.pregnancy.admin.service.support.PasswordPolicy;
import java.time.Instant;
import java.util.Collections;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

/** 不依赖数据库的安全契约测试：凭据用途、签名隔离、口令字节上限和版本头。 */
class AdminTokenTest {
  private final AdminSecurityConfig config = new AdminSecurityConfig();
  private final AdminAuthProperties properties =
      properties("unit-admin-test-secret-with-more-than-32-bytes");
  private final SecretKey key = config.adminJwtKey(properties);
  private final JwtEncoder encoder = config.adminJwtEncoder(key);
  private final JwtDecoder decoder = config.adminJwtDecoder(key, properties);

  private static AdminAuthProperties properties(String secret) {
    AdminAuthProperties properties = new AdminAuthProperties();
    properties.setSecret(secret);
    return properties;
  }

  @Test
  void adminTokenContainsNoPasswordOrRoleSnapshot() {
    Jwt jwt = decoder.decode(new AdminTokenService(encoder, properties).issue(1L, "session-id"));
    assertThat(jwt.getAudience()).containsExactly("admin");
    assertThat(jwt.getClaims())
        .doesNotContainKeys("password", "passwordHash", "roles", "permissions");
  }

  @Test
  void miniappTokenIsRejectedEvenWhenSignedWithSameKey() {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.getIssuer())
            .subject("1")
            .audience(Collections.singletonList("miniapp"))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(100))
            .claim("sid", "session-id")
            .claim("kind", "access")
            .build();
    String token =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void otherKeyCannotSignValidAdminToken() {
    JwtDecoder other =
        config.adminJwtDecoder(
            config.adminJwtKey(properties("another-separate-admin-test-secret-32-bytes")),
            properties);
    String token = new AdminTokenService(encoder, properties).issue(1L, "session-id");
    assertThatThrownBy(() -> other.decode(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void expiredJwtIsRejected() {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.getIssuer())
            .subject("1")
            .audience(Collections.singletonList("admin"))
            .issuedAt(Instant.now().minusSeconds(1000))
            .expiresAt(Instant.now().minusSeconds(500))
            .claim("sid", "session-id")
            .claim("kind", "admin_access")
            .build();
    String token =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void utf8PasswordLimitIsCheckedByBytes() {
    assertThatCode(() -> PasswordPolicy.validate("twelve-characters-password"))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> PasswordPolicy.validate(String.join("", Collections.nCopies(25, "密"))))
        .isInstanceOf(BizException.class);
  }

  @Test
  void versionHeaderRequiresQuotedPositiveInteger() {
    assertThat(RevisionHeaders.parse("\"12\"")).isEqualTo(12);
    assertThatThrownBy(() -> RevisionHeaders.parse(null))
        .isInstanceOf(BizException.class)
        .satisfies(ex -> assertThat(((BizException) ex).getStatus()).isEqualTo(428));
    assertThatThrownBy(() -> RevisionHeaders.parse("12")).isInstanceOf(BizException.class);
  }
}
