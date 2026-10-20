package com.chunyu.pregnancy.admin.controller;

import com.chunyu.pregnancy.admin.common.result.ApiResponse;
import com.chunyu.pregnancy.admin.common.util.RevisionHeaders;
import com.chunyu.pregnancy.admin.dto.request.*;
import com.chunyu.pregnancy.admin.dto.response.*;
import com.chunyu.pregnancy.admin.security.CurrentOperator;
import com.chunyu.pregnancy.admin.service.KnowledgeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 知识HTTP入口。If-Match针对目标资源：版本写入用版本revision，新增/下架用条目revision。 */
@RestController
@Validated
@SecurityRequirement(name = "adminBearer")
@RequestMapping("/admin/api/v1/knowledge")
public class KnowledgeController {
  private final KnowledgeService knowledge;
  private final com.chunyu.pregnancy.admin.service.AdminReadService read;

  public KnowledgeController(
      KnowledgeService knowledge, com.chunyu.pregnancy.admin.service.AdminReadService read) {
    this.knowledge = knowledge;
    this.read = read;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('knowledge:read')")
  public ApiResponse<PageResponse<java.util.Map<String, Object>>> list(
      @Valid @ModelAttribute AdminListQuery query) {
    return ApiResponse.ok(read.knowledge(query, "latest", CurrentOperator.id()));
  }

  @GetMapping("/{articleUuid}/versions")
  @PreAuthorize("hasAuthority('knowledge:read')")
  public ApiResponse<PageResponse<KnowledgeSummaryResponse>> versions(
      @PathVariable String articleUuid,
      @RequestParam(required = false) @Min(1) Long beforeId,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return ApiResponse.ok(knowledge.versions(articleUuid, beforeId, limit));
  }

  @GetMapping("/versions/{uuid}")
  @PreAuthorize("hasAuthority('knowledge:read')")
  public ApiResponse<KnowledgeResponse> detail(@PathVariable String uuid) {
    return ApiResponse.ok(knowledge.detail(uuid));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('knowledge:edit')")
  public ApiResponse<MutationResponse> create(
      @Valid @RequestBody CreateArticleRequest body,
      @RequestHeader(value = "Idempotency-Key", required = false) String key) {
    return ApiResponse.ok(knowledge.create(CurrentOperator.id(), key, body));
  }

  @PostMapping("/{articleUuid}/versions")
  @PreAuthorize("hasAuthority('knowledge:edit')")
  public ApiResponse<MutationResponse> newVersion(
      @PathVariable String articleUuid,
      @Valid @RequestBody NewVersionRequest body,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestHeader(value = "If-Match", required = false) String revision) {
    return ApiResponse.ok(
        knowledge.newVersion(
            CurrentOperator.id(), articleUuid, RevisionHeaders.parse(revision), key, body));
  }

  @PatchMapping("/versions/{uuid}")
  @PreAuthorize("hasAuthority('knowledge:edit')")
  public ApiResponse<MutationResponse> edit(
      @PathVariable String uuid,
      @Valid @RequestBody DraftContentRequest body,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestHeader(value = "If-Match", required = false) String revision) {
    return ApiResponse.ok(
        knowledge.edit(CurrentOperator.id(), uuid, RevisionHeaders.parse(revision), key, body));
  }

  @PostMapping("/versions/{uuid}/submit")
  @PreAuthorize("hasAuthority('knowledge:edit')")
  public ApiResponse<MutationResponse> submit(
      @PathVariable String uuid,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestHeader(value = "If-Match", required = false) String revision) {
    return ApiResponse.ok(
        knowledge.submit(CurrentOperator.id(), uuid, RevisionHeaders.parse(revision), key));
  }

  @PostMapping("/versions/{uuid}/review")
  @PreAuthorize("hasAuthority('knowledge:review')")
  public ApiResponse<MutationResponse> review(
      @PathVariable String uuid,
      @Valid @RequestBody ReviewRequest body,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestHeader(value = "If-Match", required = false) String revision) {
    return ApiResponse.ok(
        knowledge.review(CurrentOperator.id(), uuid, RevisionHeaders.parse(revision), key, body));
  }

  @PostMapping("/versions/{uuid}/publish")
  @PreAuthorize("hasAuthority('knowledge:publish')")
  public ApiResponse<MutationResponse> publish(
      @PathVariable String uuid,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestHeader(value = "If-Match", required = false) String revision) {
    return ApiResponse.ok(
        knowledge.publish(CurrentOperator.id(), uuid, RevisionHeaders.parse(revision), key));
  }

  @PostMapping("/{articleUuid}/withdraw")
  @PreAuthorize("hasAuthority('knowledge:publish')")
  public ApiResponse<MutationResponse> withdraw(
      @PathVariable String articleUuid,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestHeader(value = "If-Match", required = false) String revision) {
    return ApiResponse.ok(
        knowledge.withdraw(
            CurrentOperator.id(), articleUuid, RevisionHeaders.parse(revision), key));
  }
}
