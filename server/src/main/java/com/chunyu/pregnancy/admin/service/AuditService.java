package com.chunyu.pregnancy.admin.service;

import com.chunyu.pregnancy.admin.dto.response.*;

/** 审计只读查询契约，不提供管理人员任意删除/改写审计记录的接口。 */
public interface AuditService {
  PageResponse<AuditResponse> list(Long beforeId, int limit);
}
