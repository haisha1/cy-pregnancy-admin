package com.chunyu.pregnancy.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** 管理账号，禁止直接作为HTTP响应或输出默认toString。 */
// Lombok 在编译期生成 getter；IDE 需要启用注解处理，运行时不靠反射生成方法。
@Getter
// Lombok 在编译期生成 setter，减少字段赋值方法的重复代码。
@Setter
// 指定实体对应的数据库表名；这是映射声明，不负责自动创建表。
@TableName("admin_operator")
public class AdminOperator {
  // 标记数据库主键及生成策略；AUTO 表示使用数据库自增主键。
  @TableId(type = IdType.AUTO)
  private Long id;

  private String clientUuid;
  private String username;
  // BCrypt 散列，不是明文，也不应放入返回给浏览器的 DTO。
  private String passwordHash;
  private String displayName;
  private String state;
  // 用于并发版本比较；更新后递增，客户端通过 If-Match 携带旧版本。
  private Long revision;
  private Instant createdAt;
}
