package com.chunyu.pregnancy.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 列表仅返回摘要，避免分页时批量加载全部双表达正文。 */
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
