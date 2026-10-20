package com.chunyu.pregnancy.admin.controller;

import com.chunyu.pregnancy.admin.common.result.ApiResponse;
import com.chunyu.pregnancy.admin.dto.request.AdminListQuery;
import com.chunyu.pregnancy.admin.dto.response.PageResponse;
import com.chunyu.pregnancy.admin.security.CurrentOperator;
import com.chunyu.pregnancy.admin.service.AdminReadService;
import java.util.*;
import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 管理Web新增读入口，写动作继续走原有幂等与乐观锁状态机。 */
@RestController
@Validated
@RequestMapping("/admin/api/v1")
public class AdminReadController {
  private final AdminReadService read;

  public AdminReadController(AdminReadService read) {
    this.read = read;
  }

  @GetMapping("/knowledge/review-queue")
  @PreAuthorize("hasAuthority('knowledge:review')")
  public ApiResponse<PageResponse<Map<String, Object>>> reviews(
      @Valid @ModelAttribute AdminListQuery q) {
    return ApiResponse.ok(read.knowledge(q, "review", CurrentOperator.id()));
  }

  @GetMapping("/knowledge/release-queue")
  @PreAuthorize("hasAuthority('knowledge:publish')")
  public ApiResponse<PageResponse<Map<String, Object>>> releases(
      @Valid @ModelAttribute AdminListQuery q,
      @RequestParam(defaultValue = "approved") @Pattern(regexp = "approved|current") String view) {
    return ApiResponse.ok(
        read.knowledge(q, "current".equals(view) ? "current" : "release", CurrentOperator.id()));
  }

  @GetMapping("/operators/{uuid}")
  @PreAuthorize("hasAuthority('operator:read')")
  public ApiResponse<Map<String, Object>> operator(@PathVariable String uuid) {
    return ApiResponse.ok(read.operator(uuid));
  }

  @GetMapping("/dashboard/summary")
  @PreAuthorize("hasAuthority('dashboard:read')")
  public ApiResponse<Map<String, Object>> summary() {
    return ApiResponse.ok(read.summary(CurrentOperator.id()));
  }

  @GetMapping("/dashboard/user-trend")
  @PreAuthorize("hasAuthority('user:read')")
  public ApiResponse<Map<String, Object>> trend(
      @RequestParam(defaultValue = "7") @Min(1) @Max(30) int days) {
    return ApiResponse.ok(read.userStats(days));
  }

  @GetMapping("/dashboard/recent-activity")
  @PreAuthorize("hasAuthority('audit:read')")
  public ApiResponse<PageResponse<Map<String, Object>>> activity() {
    AdminListQuery q = new AdminListQuery();
    q.setLimit(5);
    return ApiResponse.ok(read.audits(q));
  }

  @GetMapping("/users")
  @PreAuthorize("hasAuthority('user:read')")
  public ApiResponse<Map<String, Object>> users(@Valid @ModelAttribute AdminListQuery q) {
    return ApiResponse.ok(read.users(q));
  }

  @GetMapping("/users/{uuid}")
  @PreAuthorize("hasAuthority('user:read')")
  public ApiResponse<Map<String, Object>> user(@PathVariable String uuid) {
    return ApiResponse.ok(read.user(uuid));
  }

  @GetMapping("/system/health")
  @PreAuthorize("hasAuthority('operator:read')")
  public ApiResponse<Map<String, Object>> health() {
    return ApiResponse.ok(read.health());
  }
}
