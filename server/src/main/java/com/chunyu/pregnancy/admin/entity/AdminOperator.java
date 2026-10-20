package com.chunyu.pregnancy.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** 管理账号，禁止直接作为HTTP响应或输出默认toString。 */
@Getter
@Setter
@TableName("admin_operator")
public class AdminOperator {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String clientUuid;
  private String username;
  private String passwordHash;
  private String displayName;
  private String state;
  private Long revision;
  private Instant createdAt;
}
