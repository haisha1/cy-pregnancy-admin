package com.chunyu.pregnancy.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 列表仅返回摘要，避免分页时批量加载全部双表达正文。 */
// Lombok 在编译期生成 getter；IDE 需要启用注解处理，运行时不靠反射生成方法。
@Getter
@AllArgsConstructor
public class KnowledgeSummaryResponse {
  private final String articleUuid;
  private final String slug;
  private final long articleRevision;
  private final String uuid;
  private final int versionNo;
  private final String title;
  private final String category;
  private final String state;
  private final long revision;
  private final boolean currentlyPublished;
}
