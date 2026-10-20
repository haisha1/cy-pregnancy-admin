package com.chunyu.pregnancy.admin.security;

import com.chunyu.pregnancy.admin.config.AdminAuthProperties;
import java.time.Instant;
import java.util.Collections;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

/** 管理JWT只声明身份和会话，不把权限塞入长期凭据，避免角色变更后旧权限继续有效。 */
@Service
public class AdminTokenService {
  private final JwtEncoder encoder;
  private final AdminAuthProperties properties;

  public AdminTokenService(JwtEncoder encoder, AdminAuthProperties properties) {
    this.encoder = encoder;
    this.properties = properties;
  }

  public String issue(Long operatorId, String sessionUuid) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.getIssuer())
            .subject(operatorId.toString())
            .audience(Collections.singletonList("admin"))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(properties.getAccessSeconds()))
            .claim("sid", sessionUuid)
            .claim("kind", "admin_access")
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
