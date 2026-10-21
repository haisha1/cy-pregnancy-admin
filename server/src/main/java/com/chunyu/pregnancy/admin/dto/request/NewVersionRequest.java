package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;

/** 已有条目新增完整草稿，不覆盖已审核或已发布版本。 */
public class NewVersionRequest extends DraftContentRequest {
  // 要求文本非 null 且不能全是空白字符，用于必填文本。
  @NotBlank public String clientUuid;
}
