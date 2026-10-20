package com.chunyu.pregnancy.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 管理端不提供刷新接口；到期后重新登录，默认会话30分钟。 */
@Getter
@AllArgsConstructor
public class TokenResponse {
  private final String accessToken;
  private final String tokenType;
  private final long expiresIn;
}
