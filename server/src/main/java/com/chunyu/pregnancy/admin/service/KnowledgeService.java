package com.chunyu.pregnancy.admin.service;

import com.chunyu.pregnancy.admin.dto.request.*;
import com.chunyu.pregnancy.admin.dto.response.*;

/** 知识管理契约。发布只作用于管理库，用户端消费已发布版本的服务链路后续单独接入。 */
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
