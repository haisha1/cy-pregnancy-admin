package com.chunyu.pregnancy.admin.dto.request;

import java.util.List;
import javax.validation.constraints.*;

/** 修改账号状态和角色，服务端同时校验最后一名安全管理员保护。 */
public class UpdateOperatorRequest {
  @NotBlank
  @Size(max = 64)
  public String displayName;

  @NotBlank
  @Pattern(regexp = "active|disabled")
  public String state;

  @NotNull
  @Size(min = 1, max = 4)
  public List<@NotBlank String> roleCodes;
}
