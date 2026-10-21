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
// HTTP 控制器：接收请求，返回值由框架序列化为响应，通常是 JSON。
@RestController
// 启用此处的参数约束校验；配置类可校验配置值，Controller 可校验方法参数。
@Validated
// 声明 OpenAPI 文档中的认证要求，不会代替 Spring Security 的实际拦截。
@SecurityRequirement(name = "adminBearer")
// 声明接口路径；类上的公共前缀会与方法上的路径拼接。
@RequestMapping("/admin/api/v1")
public class AdminOperatorController {
  private final AdminOperatorService operators;
  private final com.chunyu.pregnancy.admin.service.AdminReadService read;

  // 单一构造器由 Spring 自动注入依赖；final 字段表明这些依赖在构造后不再替换。
  public AdminOperatorController(
      AdminOperatorService operators, com.chunyu.pregnancy.admin.service.AdminReadService read) {
    this.operators = operators;
    this.read = read;
  }

  // 将下面的方法映射为 GET 查询接口，不在此声明中处理数据库事务。
  @GetMapping("/operators")
  // 方法执行前检查当前认证信息中的权限；隐藏前端按钮不能替代这里的服务端检查。
  @PreAuthorize("hasAuthority('operator:read')")
  public ApiResponse<PageResponse<java.util.Map<String, Object>>> list(
      // 校验当前对象内部字段上的约束，必要时继续递归校验嵌套 DTO。
      @Valid @ModelAttribute AdminListQuery query) {
    return ApiResponse.ok(read.operators(query));
  }

  @GetMapping("/roles")
  @PreAuthorize("hasAuthority('operator:read')")
  public ApiResponse<List<RoleResponse>> roles() {
    return ApiResponse.ok(operators.roles());
  }

  // 将下面的方法映射为 POST 接口；请求体解析和业务处理由参数及方法体决定。
  @PostMapping("/operators")
  @PreAuthorize("hasAuthority('operator:manage')")
  public ApiResponse<MutationResponse> create(
      @Valid @RequestBody CreateOperatorRequest body,
      // 读取指定 HTTP 请求头，本项目用它传递幂等标识或并发版本等信息。
      @RequestHeader(value = "Idempotency-Key", required = false) String key) {
    return ApiResponse.ok(operators.create(CurrentOperator.id(), key, body));
  }

  // 将下面的方法映射为 PATCH 更新接口，具体允许修改哪些字段由 DTO 和业务层限制。
  @PatchMapping("/operators/{uuid}")
  @PreAuthorize("hasAuthority('operator:manage')")
  public ApiResponse<MutationResponse> update(
      // 读取 URL 路径中的变量，例如知识版本 UUID；不是从 JSON 请求体读取。
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
