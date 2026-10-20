package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;

/** 重置或变更密码后撤销目标账号的全部会话。 */
public class ResetPasswordRequest {
  @NotBlank
  @Size(min = 12, max = 72)
  public String password;
}
