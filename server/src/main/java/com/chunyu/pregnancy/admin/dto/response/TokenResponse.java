package com.chunyu.pregnancy.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 管理端不提供刷新接口；到期后重新登录，默认会话30分钟。 */
// Lombok 在编译期生成 getter；IDE 需要启用注解处理，运行时不靠反射生成方法。
@Getter
@AllArgsConstructor
public class TokenResponse {
  private final String accessToken;
  private final String tokenType;
  private final long expiresIn;
}
