package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;

/** 已有条目新增完整草稿，不覆盖已审核或已发布版本。 */
public class NewVersionRequest extends DraftContentRequest {
  @NotBlank public String clientUuid;
}
