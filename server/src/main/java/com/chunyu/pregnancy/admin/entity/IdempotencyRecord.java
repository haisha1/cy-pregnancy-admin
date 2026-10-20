package com.chunyu.pregnancy.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** 只保存管理命令的HMAC摘要和最小结果回执，不保存请求体。 */
@Getter
@Setter
@TableName("admin_idempotency")
public class IdempotencyRecord {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long actorId;
  private String commandScope;
  private String operationUuid;
  private String payloadHash;
  private String responseJson;
  private Instant createdAt;
}
