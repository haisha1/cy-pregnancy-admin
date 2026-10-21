package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;

/** 管理员密码登录入参。用户名固定为小写ASCII，避免身份大小写歧义。 */
public class LoginRequest {
  // 要求文本非 null 且不能全是空白字符，用于必填文本。
  @NotBlank
  // 要求非 null 文本匹配指定正则；用户名、状态值等只接受约定格式。
  @Pattern(regexp = "[a-z][a-z0-9_]{2,39}")
  public String username;

  @NotBlank
  // 限制非 null 文本的长度或集合大小；是否必填还需配合 NotNull、NotBlank 等约束。
  @Size(max = 72)
  public String password;
}
