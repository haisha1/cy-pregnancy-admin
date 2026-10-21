package com.chunyu.pregnancy.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** 只保存管理命令的HMAC摘要和最小结果回执，不保存请求体。 */
// Lombok 在编译期生成 getter；IDE 需要启用注解处理，运行时不靠反射生成方法。
@Getter
// Lombok 在编译期生成 setter，减少字段赋值方法的重复代码。
@Setter
// 指定实体对应的数据库表名；这是映射声明，不负责自动创建表。
@TableName("admin_idempotency")
public class IdempotencyRecord {
  // 标记数据库主键及生成策略；AUTO 表示使用数据库自增主键。
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long actorId;
  private String commandScope;
  private String operationUuid;
  private String payloadHash;
  private String responseJson;
  private Instant createdAt;
}
