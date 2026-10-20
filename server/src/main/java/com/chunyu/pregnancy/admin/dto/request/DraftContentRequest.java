package com.chunyu.pregnancy.admin.dto.request;

import java.util.List;
import javax.validation.constraints.*;

/** 双表达内容作为同一版本提交；这里的文本不执行HTML或远程公式。 */
public class DraftContentRequest {
  @NotBlank
  @Size(max = 160)
  public String title;

  @NotBlank
  @Size(max = 64)
  public String category;

  @NotBlank
  @Size(max = 20000)
  public String proContent;

  @NotBlank
  @Size(max = 20000)
  public String plainContent;

  @NotNull
  @Size(max = 30)
  public List<@NotBlank @Size(max = 500) String> references;

  @NotBlank
  @Size(max = 2000)
  public String evidenceNote;
}
