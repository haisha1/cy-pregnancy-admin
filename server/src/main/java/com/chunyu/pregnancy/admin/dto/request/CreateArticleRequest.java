package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;

/** 原子创建条目和第一份草稿；两个UUID由调用端生成并在重试时保持。 */
public class CreateArticleRequest extends DraftContentRequest {
  // 要求文本非 null 且不能全是空白字符，用于必填文本。
  @NotBlank public String clientUuid;
  @NotBlank public String versionUuid;

  @NotBlank
  // 要求非 null 文本匹配指定正则；用户名、状态值等只接受约定格式。
  @Pattern(regexp = "[a-z0-9][a-z0-9-]{2,99}")
  public String slug;
}
