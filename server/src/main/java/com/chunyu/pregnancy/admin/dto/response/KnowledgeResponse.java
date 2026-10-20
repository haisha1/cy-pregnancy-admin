package com.chunyu.pregnancy.admin.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** 带明确条目、内容版本与乐观锁的知识详情；没有公开永久下载地址。 */
@Getter
@Setter
public class KnowledgeResponse {
  private String articleUuid;
  private String slug;
  private long articleRevision;
  private String uuid;
  private int versionNo;
  private String title;
  private String category;
  private String proContent;
  private String plainContent;
  private List<String> references;
  private String evidenceNote;
  private String state;
  private String authorUuid;
  private String reviewerUuid;
  private String reviewNote;
  private Instant publishedAt;
  private long revision;
  private boolean currentlyPublished;
}
