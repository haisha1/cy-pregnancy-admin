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
// HTTP 控制器：接收请求，返回值由框架序列化为响应，通常是 JSON。
@RestController
// 启用此处的参数约束校验；配置类可校验配置值，Controller 可校验方法参数。
@Validated
// 声明 OpenAPI 文档中的认证要求，不会代替 Spring Security 的实际拦截。
@SecurityRequirement(name = "adminBearer")
// 声明接口路径；类上的公共前缀会与方法上的路径拼接。
@RequestMapping("/admin/api/v1/knowledge")
public class KnowledgeController {
  private final KnowledgeService knowledge;
  private final com.chunyu.pregnancy.admin.service.AdminReadService read;

  // 单一构造器由 Spring 自动注入依赖；final 字段表明这些依赖在构造后不再替换。
  public KnowledgeController(
      KnowledgeService knowledge, com.chunyu.pregnancy.admin.service.AdminReadService read) {
    this.knowledge = knowledge;
    this.read = read;
  }

  // 将下面的方法映射为 GET 查询接口，不在此声明中处理数据库事务。
  @GetMapping
  // 方法执行前检查当前认证信息中的权限；隐藏前端按钮不能替代这里的服务端检查。
  @PreAuthorize("hasAuthority('knowledge:read')")
  public ApiResponse<PageResponse<java.util.Map<String, Object>>> list(
      // 校验当前对象内部字段上的约束，必要时继续递归校验嵌套 DTO。
      @Valid @ModelAttribute AdminListQuery query) {
    return ApiResponse.ok(read.knowledge(query, "latest", CurrentOperator.id()));
  }

  @GetMapping("/{articleUuid}/versions")
  @PreAuthorize("hasAuthority('knowledge:read')")
  public ApiResponse<PageResponse<KnowledgeSummaryResponse>> versions(
      // 读取 URL 路径中的变量，例如知识版本 UUID；不是从 JSON 请求体读取。
      @PathVariable String articleUuid,
      // 读取查询参数或表单参数，默认值和必填要求以注解参数为准。
      @RequestParam(required = false) @Min(1) Long beforeId,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
    return ApiResponse.ok(knowledge.versions(articleUuid, beforeId, limit));
  }

  @GetMapping("/versions/{uuid}")
  @PreAuthorize("hasAuthority('knowledge:read')")
  public ApiResponse<KnowledgeResponse> detail(@PathVariable String uuid) {
    return ApiResponse.ok(knowledge.detail(uuid));
  }

  // 将下面的方法映射为 POST 接口；请求体解析和业务处理由参数及方法体决定。
  @PostMapping
  @PreAuthorize("hasAuthority('knowledge:edit')")
  public ApiResponse<MutationResponse> create(
      @Valid @RequestBody CreateArticleRequest body,
      // 读取指定 HTTP 请求头，本项目用它传递幂等标识或并发版本等信息。
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

  // 将下面的方法映射为 PATCH 更新接口，具体允许修改哪些字段由 DTO 和业务层限制。
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
