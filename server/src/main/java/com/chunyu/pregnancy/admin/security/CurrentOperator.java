package com.chunyu.pregnancy.admin.security;

import com.chunyu.pregnancy.admin.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** 从认证上下文取得操作者，禁止由请求body传actorId模拟其他管理人员。 */
public final class CurrentOperator {
  private CurrentOperator() {}

  public static Long id() {
    return Long.valueOf(jwt().getSubject());
  }

  public static String sessionUuid() {
    return jwt().getClaimAsString("sid");
  }

  private static Jwt jwt() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
      throw new BizException(401, "AUTH_EXPIRED", "请先登录管理端");
    }
    return (Jwt) authentication.getPrincipal();
  }
}
