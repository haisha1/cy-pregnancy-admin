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
// HTTP 控制器：接收请求，返回值由框架序列化为响应，通常是 JSON。
@RestController
// 启用此处的参数约束校验；配置类可校验配置值，Controller 可校验方法参数。
@Validated
// 声明接口路径；类上的公共前缀会与方法上的路径拼接。
@RequestMapping("/admin/api/v1")
public class AdminReadController {
  private final AdminReadService read;

  // 单一构造器由 Spring 自动注入依赖；final 字段表明这些依赖在构造后不再替换。
  public AdminReadController(AdminReadService read) {
    this.read = read;
  }

  // 将下面的方法映射为 GET 查询接口，不在此声明中处理数据库事务。
  @GetMapping("/knowledge/review-queue")
  // 方法执行前检查当前认证信息中的权限；隐藏前端按钮不能替代这里的服务端检查。
  @PreAuthorize("hasAuthority('knowledge:review')")
  public ApiResponse<PageResponse<Map<String, Object>>> reviews(
      // 校验当前对象内部字段上的约束，必要时继续递归校验嵌套 DTO。
      @Valid @ModelAttribute AdminListQuery q) {
    return ApiResponse.ok(read.knowledge(q, "review", CurrentOperator.id()));
  }

  @GetMapping("/knowledge/release-queue")
  @PreAuthorize("hasAuthority('knowledge:publish')")
  public ApiResponse<PageResponse<Map<String, Object>>> releases(
      @Valid @ModelAttribute AdminListQuery q,
      // 读取查询参数或表单参数，默认值和必填要求以注解参数为准。
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
