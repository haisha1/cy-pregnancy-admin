package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;

/** 原子创建条目和第一份草稿；两个UUID由调用端生成并在重试时保持。 */
public class CreateArticleRequest extends DraftContentRequest {
  @NotBlank public String clientUuid;
  @NotBlank public String versionUuid;

  @NotBlank
  @Pattern(regexp = "[a-z0-9][a-z0-9-]{2,99}")
  public String slug;
}
