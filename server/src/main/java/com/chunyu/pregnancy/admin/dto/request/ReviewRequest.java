package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;

/** 审核需明确决定和说明；作者不可审核自己编写的版本。 */
public class ReviewRequest {
  @NotBlank
  @Pattern(regexp = "approve|reject")
  public String decision;

  @NotBlank
  @Size(max = 2000)
  public String note;
}
