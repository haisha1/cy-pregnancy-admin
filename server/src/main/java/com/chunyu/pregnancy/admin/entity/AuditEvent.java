package com.chunyu.pregnancy.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** 最小审计元数据，不保存密码、token、内容正文或健康数据。 */
@Getter
@Setter
@TableName("admin_audit_event")
public class AuditEvent {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long actorId;
  private String resourceType;
  private String resourceUuid;
  private String action;
  private String resultCode;
  private String requestId;
  private Instant occurredAt;
}
