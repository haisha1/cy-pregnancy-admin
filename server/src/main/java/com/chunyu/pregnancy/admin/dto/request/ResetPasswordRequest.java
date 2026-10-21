package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;

/** 重置或变更密码后撤销目标账号的全部会话。 */
public class ResetPasswordRequest {
  // 要求文本非 null 且不能全是空白字符，用于必填文本。
  @NotBlank
  // 限制非 null 文本的长度或集合大小；是否必填还需配合 NotNull、NotBlank 等约束。
  @Size(min = 12, max = 72)
  public String password;
}
