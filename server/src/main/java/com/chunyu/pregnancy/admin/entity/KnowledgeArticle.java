package com.chunyu.pregnancy.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** 知识条目身份与当前发布指针。历史版本永不被新内容覆盖。 */
@Getter
@Setter
@TableName("admin_knowledge_article")
public class KnowledgeArticle {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String clientUuid;
  private String slug;
  private Long publishedVersionId;
  private Long revision;
  private Instant createdAt;
}
