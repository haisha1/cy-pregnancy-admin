package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;

/** 管理员密码登录入参。用户名固定为小写ASCII，避免身份大小写歧义。 */
public class LoginRequest {
  @NotBlank
  @Pattern(regexp = "[a-z][a-z0-9_]{2,39}")
  public String username;

  @NotBlank
  @Size(max = 72)
  public String password;
}
