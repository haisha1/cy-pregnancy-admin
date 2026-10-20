package com.chunyu.pregnancy.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** 独立管理会话，JWT过期前也可以被撤销。 */
@Getter
@Setter
@TableName("admin_session")
public class AdminSession {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String clientUuid;
  private Long operatorId;
  private Instant expiresAt;
  private Instant revokedAt;
  private Instant createdAt;
}
