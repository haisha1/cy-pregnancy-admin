package com.chunyu.pregnancy.admin.dto.request;

import java.util.List;
import javax.validation.constraints.*;

/** 创建管理账号。角色代码必须来自预置清单，不支持任意权限字符串。 */
public class CreateOperatorRequest {
  @NotBlank public String clientUuid;

  @NotBlank
  @Pattern(regexp = "[a-z][a-z0-9_]{2,39}")
  public String username;

  @NotBlank
  @Size(max = 64)
  public String displayName;

  @NotBlank
  @Size(min = 12, max = 72)
  public String password;

  @NotNull
  @Size(min = 1, max = 4)
  public List<@NotBlank String> roleCodes;
}
