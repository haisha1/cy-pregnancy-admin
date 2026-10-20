package com.chunyu.pregnancy.admin.service;

import com.chunyu.pregnancy.admin.dto.request.*;
import com.chunyu.pregnancy.admin.dto.response.*;
import java.util.List;

/** 管理账号和预置角色管理；数据库实体不越过服务层直接返回。 */
public interface AdminOperatorService {
  PageResponse<OperatorResponse> list(Long beforeId, int limit);

  List<RoleResponse> roles();

  MutationResponse create(Long actorId, String operationUuid, CreateOperatorRequest request);

  MutationResponse update(
      Long actorId,
      String uuid,
      long expected,
      String operationUuid,
      UpdateOperatorRequest request);

  MutationResponse resetPassword(
      Long actorId, String uuid, long expected, String operationUuid, ResetPasswordRequest request);
}
