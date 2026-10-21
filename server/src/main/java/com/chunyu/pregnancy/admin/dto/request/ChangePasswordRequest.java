package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;

/** 本人改密必须验证旧密码，避免只靠被盗的accessToken持久接管账号。 */
public class ChangePasswordRequest {
  // 要求文本非 null 且不能全是空白字符，用于必填文本。
  @NotBlank
  // 限制非 null 文本的长度或集合大小；是否必填还需配合 NotNull、NotBlank 等约束。
  @Size(max = 72)
  public String oldPassword;

  @NotBlank
  @Size(min = 12, max = 72)
  public String newPassword;
}
