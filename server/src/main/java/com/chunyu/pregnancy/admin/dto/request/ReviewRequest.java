package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;

/** 审核需明确决定和说明；作者不可审核自己编写的版本。 */
public class ReviewRequest {
  // 要求文本非 null 且不能全是空白字符，用于必填文本。
  @NotBlank
  // 要求非 null 文本匹配指定正则；用户名、状态值等只接受约定格式。
  @Pattern(regexp = "approve|reject")
  public String decision;

  @NotBlank
  // 限制非 null 文本的长度或集合大小；是否必填还需配合 NotNull、NotBlank 等约束。
  @Size(max = 2000)
  public String note;
}
