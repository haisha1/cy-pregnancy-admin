package com.chunyu.pregnancy.admin.dto.request;

import java.util.List;
import javax.validation.constraints.*;

/** 创建管理账号。角色代码必须来自预置清单，不支持任意权限字符串。 */
public class CreateOperatorRequest {
  // 要求文本非 null 且不能全是空白字符，用于必填文本。
  @NotBlank public String clientUuid;

  @NotBlank
  // 要求非 null 文本匹配指定正则；用户名、状态值等只接受约定格式。
  @Pattern(regexp = "[a-z][a-z0-9_]{2,39}")
  public String username;

  @NotBlank
  // 限制非 null 文本的长度或集合大小；是否必填还需配合 NotNull、NotBlank 等约束。
  @Size(max = 64)
  public String displayName;

  @NotBlank
  @Size(min = 12, max = 72)
  public String password;

  // 要求值不能为 null；它本身不限制字符串是否为空或集合是否为空。
  @NotNull
  @Size(min = 1, max = 4)
  public List<@NotBlank String> roleCodes;
}
