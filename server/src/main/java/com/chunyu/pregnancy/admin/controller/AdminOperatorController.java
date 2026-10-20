package com.chunyu.pregnancy.admin.controller;

import com.chunyu.pregnancy.admin.common.result.ApiResponse;
import com.chunyu.pregnancy.admin.common.util.RevisionHeaders;
import com.chunyu.pregnancy.admin.dto.request.*;
import com.chunyu.pregnancy.admin.dto.response.*;
import com.chunyu.pregnancy.admin.security.CurrentOperator;
import com.chunyu.pregnancy.admin.service.AdminOperatorService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 账号/角色管理。写权限在入口检查，并在Service持锁后再次检查，防止并发降权绕过。 */
@RestController
@Validated
@SecurityRequirement(name = "adminBearer")
@RequestMapping("/admin/api/v1")
public class AdminOperatorController {
  private final AdminOperatorService operators;
  private final com.chunyu.pregnancy.admin.service.AdminReadService read;

  public AdminOperatorController(
      AdminOperatorService operators, com.chunyu.pregnancy.admin.service.AdminReadService read) {
    this.operators = operators;
    this.read = read;
  }

  @GetMapping("/operators")
  @PreAuthorize("hasAuthority('operator:read')")
  public ApiResponse<PageResponse<java.util.Map<String, Object>>> list(
      @Valid @ModelAttribute AdminListQuery query) {
    return ApiResponse.ok(read.operators(query));
  }

  @GetMapping("/roles")
  @PreAuthorize("hasAuthority('operator:read')")
  public ApiResponse<List<RoleResponse>> roles() {
    return ApiResponse.ok(operators.roles());
  }

  @PostMapping("/operators")
  @PreAuthorize("hasAuthority('operator:manage')")
  public ApiResponse<MutationResponse> create(
      @Valid @RequestBody CreateOperatorRequest body,
      @RequestHeader(value = "Idempotency-Key", required = false) String key) {
    return ApiResponse.ok(operators.create(CurrentOperator.id(), key, body));
  }

  @PatchMapping("/operators/{uuid}")
  @PreAuthorize("hasAuthority('operator:manage')")
  public ApiResponse<MutationResponse> update(
      @PathVariable String uuid,
      @Valid @RequestBody UpdateOperatorRequest body,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestHeader(value = "If-Match", required = false) String revision) {
    return ApiResponse.ok(
        operators.update(CurrentOperator.id(), uuid, RevisionHeaders.parse(revision), key, body));
  }

  @PostMapping("/operators/{uuid}/password")
  @PreAuthorize("hasAuthority('operator:manage')")
  public ApiResponse<MutationResponse> reset(
      @PathVariable String uuid,
      @Valid @RequestBody ResetPasswordRequest body,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestHeader(value = "If-Match", required = false) String revision) {
    return ApiResponse.ok(
        operators.resetPassword(
            CurrentOperator.id(), uuid, RevisionHeaders.parse(revision), key, body));
  }
}
