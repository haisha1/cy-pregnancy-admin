package com.chunyu.pregnancy.admin.service;

import com.chunyu.pregnancy.admin.dto.request.*;
import com.chunyu.pregnancy.admin.dto.response.*;

/** 知识管理契约。负责管理库中的版本状态和发布指针，用户服务通过独立内部只读接口消费当前发布内容。 */
public interface KnowledgeService {
  PageResponse<KnowledgeSummaryResponse> list(Long beforeId, int limit);

  PageResponse<KnowledgeSummaryResponse> versions(String articleUuid, Long beforeId, int limit);

  KnowledgeResponse detail(String versionUuid);

  MutationResponse create(Long actor, String key, CreateArticleRequest request);

  MutationResponse newVersion(
      Long actor, String articleUuid, long expected, String key, NewVersionRequest request);

  MutationResponse edit(
      Long actor, String uuid, long expected, String key, DraftContentRequest request);

  MutationResponse submit(Long actor, String uuid, long expected, String key);

  MutationResponse review(
      Long actor, String uuid, long expected, String key, ReviewRequest request);

  MutationResponse publish(Long actor, String uuid, long expected, String key);

  MutationResponse withdraw(Long actor, String articleUuid, long expected, String key);
}
