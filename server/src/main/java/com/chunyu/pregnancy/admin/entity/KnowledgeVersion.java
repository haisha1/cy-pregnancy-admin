package com.chunyu.pregnancy.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** 一版知识的双表达内容和审核记录，送审后内容不可就地改写。 */
@Getter
@Setter
@TableName("admin_knowledge_version")
public class KnowledgeVersion {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String clientUuid;
  private Long articleId;
  private Integer versionNo;
  private String title;
  private String category;
  private String proContent;
  private String plainContent;
  private String referencesJson;
  private String evidenceNote;
  private String state;
  private Long authorId;
  private Long reviewerId;
  private String reviewNote;
  private Long publisherId;
  private Instant publishedAt;
  private Long revision;
  private Instant createdAt;
}
