package com.chunyu.pregnancy.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.dto.response.*;
import com.chunyu.pregnancy.admin.entity.AuditEvent;
import com.chunyu.pregnancy.admin.mapper.AuditEventMapper;
import com.chunyu.pregnancy.admin.service.AuditService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 审计采用ID游标倒序，查询不会扫描/返回业务正文、密码或凭据。 */
@Service
public class AuditServiceImpl implements AuditService {
  private final AuditEventMapper events;

  public AuditServiceImpl(AuditEventMapper events) {
    this.events = events;
  }

  @Override
  public PageResponse<AuditResponse> list(Long beforeId, int limit) {
    if (limit < 1 || limit > 100) {
      throw new BizException(422, "PAGE_INVALID", "每页数量必须为1–100");
    }
    List<AuditEvent> rows =
        events.selectList(
            new LambdaQueryWrapper<AuditEvent>()
                .lt(beforeId != null, AuditEvent::getId, beforeId)
                .orderByDesc(AuditEvent::getId)
                .last("LIMIT " + (limit + 1)));
    boolean more = rows.size() > limit;
    if (more) {
      rows = rows.subList(0, limit);
    }
    List<AuditResponse> records =
        rows.stream()
            .map(
                event ->
                    new AuditResponse(
                        event.getId(),
                        event.getActorId(),
                        event.getResourceType(),
                        event.getResourceUuid(),
                        event.getAction(),
                        event.getResultCode(),
                        event.getRequestId(),
                        event.getOccurredAt()))
            .collect(Collectors.toList());
    return new PageResponse<>(records, more ? rows.get(rows.size() - 1).getId() : null);
  }
}
