package com.chunyu.pregnancy.admin.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 仅可授权读取的审计元数据，账号/资源内部ID在管理域内使用。 */
@Getter
@AllArgsConstructor
public class AuditResponse {
  private final long id;
  private final Long actorId;
  private final String resourceType;
  private final String resourceUuid;
  private final String action;
  private final String resultCode;
  private final String requestId;
  private final Instant occurredAt;
}
