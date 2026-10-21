package com.chunyu.pregnancy.admin.dto.request;

import java.util.List;
import javax.validation.constraints.*;

/** 双表达内容作为同一版本提交；这里的文本不执行HTML或远程公式。 */
public class DraftContentRequest {
  // 要求文本非 null 且不能全是空白字符，用于必填文本。
  @NotBlank
  // 限制非 null 文本的长度或集合大小；是否必填还需配合 NotNull、NotBlank 等约束。
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

  // 要求值不能为 null；它本身不限制字符串是否为空或集合是否为空。
  @NotNull
  @Size(max = 30)
  public List<@NotBlank @Size(max = 500) String> references;

  @NotBlank
  @Size(max = 2000)
  public String evidenceNote;
}
