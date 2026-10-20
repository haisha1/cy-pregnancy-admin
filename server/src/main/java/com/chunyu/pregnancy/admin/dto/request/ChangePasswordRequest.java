package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;

/** 本人改密必须验证旧密码，避免只靠被盗的accessToken持久接管账号。 */
public class ChangePasswordRequest {
  @NotBlank
  @Size(max = 72)
  public String oldPassword;

  @NotBlank
  @Size(min = 12, max = 72)
  public String newPassword;
}
