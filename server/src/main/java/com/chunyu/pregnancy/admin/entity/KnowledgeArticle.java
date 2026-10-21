package com.chunyu.pregnancy.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** 知识条目身份与当前发布指针。历史版本永不被新内容覆盖。 */
// Lombok 在编译期生成 getter；IDE 需要启用注解处理，运行时不靠反射生成方法。
@Getter
// Lombok 在编译期生成 setter，减少字段赋值方法的重复代码。
@Setter
// 指定实体对应的数据库表名；这是映射声明，不负责自动创建表。
@TableName("admin_knowledge_article")
public class KnowledgeArticle {
  // 标记数据库主键及生成策略；AUTO 表示使用数据库自增主键。
  @TableId(type = IdType.AUTO)
  private Long id;

  private String clientUuid;
  private String slug;
  // 当前对外发布版本的内部 ID；为空表示当前没有可发布给用户端的版本。
  private Long publishedVersionId;
  private Long revision;
  private Instant createdAt;
}
