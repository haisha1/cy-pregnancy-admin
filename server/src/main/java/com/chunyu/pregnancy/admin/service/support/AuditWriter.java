package com.chunyu.pregnancy.admin.service.support;

import com.chunyu.pregnancy.admin.entity.AuditEvent;
import com.chunyu.pregnancy.admin.mapper.AuditEventMapper;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** 成功管理写操作在业务事务中记审计；失败登录只记最小结果，不存凭据和正文。 */
// 通用 Spring 组件：由容器创建并管理，其他类可通过构造器使用它。
@Component
public class AuditWriter {
  private final AuditEventMapper events;

  // 单一构造器由 Spring 自动注入依赖；final 字段表明这些依赖在构造后不再替换。
  public AuditWriter(AuditEventMapper events) {
    this.events = events;
  }

  public void record(Long actor, String type, String uuid, String action, String result) {
    AuditEvent event = new AuditEvent();
    event.setActorId(actor);
    event.setResourceType(type);
    event.setResourceUuid(uuid);
    event.setAction(action);
    event.setResultCode(result);
    event.setRequestId(MDC.get("requestId"));
    event.setOccurredAt(Instant.now());
    events.insert(event);
  }
}
