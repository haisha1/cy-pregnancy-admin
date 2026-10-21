package com.chunyu.pregnancy.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** 一版知识的双表达内容和审核记录，送审后内容不可就地改写。 */
// Lombok 在编译期生成 getter；IDE 需要启用注解处理，运行时不靠反射生成方法。
@Getter
// Lombok 在编译期生成 setter，减少字段赋值方法的重复代码。
@Setter
// 指定实体对应的数据库表名；这是映射声明，不负责自动创建表。
@TableName("admin_knowledge_version")
public class KnowledgeVersion {
  // 标记数据库主键及生成策略；AUTO 表示使用数据库自增主键。
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
