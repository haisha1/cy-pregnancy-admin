package com.chunyu.pregnancy.admin.controller;

import com.chunyu.pregnancy.admin.common.ratelimit.AdminLoginRateLimiter;
import com.chunyu.pregnancy.admin.common.result.ApiResponse;
import com.chunyu.pregnancy.admin.common.util.RevisionHeaders;
import com.chunyu.pregnancy.admin.dto.request.*;
import com.chunyu.pregnancy.admin.dto.response.*;
import com.chunyu.pregnancy.admin.security.CurrentOperator;
import com.chunyu.pregnancy.admin.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 账号HTTP入口只处理参数与返回，不直接调用Mapper或返回Entity。 */
// HTTP 控制器：接收请求，返回值由框架序列化为响应，通常是 JSON。
@RestController
// 声明接口路径；类上的公共前缀会与方法上的路径拼接。
@RequestMapping("/admin/api/v1")
public class AdminAuthController {
  private final AdminAuthService auth;
  private final AdminLoginRateLimiter limiter;

  // 单一构造器由 Spring 自动注入依赖；final 字段表明这些依赖在构造后不再替换。
  public AdminAuthController(AdminAuthService auth, AdminLoginRateLimiter limiter) {
    this.auth = auth;
    this.limiter = limiter;
  }

  // 将下面的方法映射为 POST 接口；请求体解析和业务处理由参数及方法体决定。
  // 参数中的 RequestBody 将 JSON 转成登录 DTO，Valid 再执行字段约束；通过后才进入方法体。
  @PostMapping("/auth/login")
  // OpenAPI 接口说明，仅描述用途；参数校验和鉴权仍由其他注解及代码执行。
  @Operation(summary = "管理员密码登录，独立于小程序微信登录")
  public ApiResponse<TokenResponse> login(
      // 校验当前对象内部字段上的约束，必要时继续递归校验嵌套 DTO。
      @Valid @RequestBody LoginRequest body, HttpServletRequest request) {
    limiter.check(request.getRemoteAddr(), body.username);
    return ApiResponse.ok(auth.login(body));
  }

  // 将下面的方法映射为 GET 查询接口，不在此声明中处理数据库事务。
  @GetMapping("/me")
  // 声明 OpenAPI 文档中的认证要求，不会代替 Spring Security 的实际拦截。
  @SecurityRequirement(name = "adminBearer")
  @Operation(summary = "读取当前管理账号与实时权限")
  public ApiResponse<MeResponse> me() {
    return ApiResponse.ok(auth.me(CurrentOperator.id()));
  }

  @PostMapping("/auth/logout")
  @SecurityRequirement(name = "adminBearer")
  @Operation(summary = "立即撤销当前管理会话")
  public ApiResponse<Void> logout() {
    auth.logout(CurrentOperator.id(), CurrentOperator.sessionUuid());
    return ApiResponse.ok(null);
  }

  @PostMapping("/auth/password")
  @SecurityRequirement(name = "adminBearer")
  @Operation(summary = "本人改密并撤销全部会话；需幂等键和If-Match")
  public ApiResponse<MutationResponse> changePassword(
      @Valid @RequestBody ChangePasswordRequest body,
      // 读取指定 HTTP 请求头，本项目用它传递幂等标识或并发版本等信息。
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestHeader(value = "If-Match", required = false) String revision) {
    return ApiResponse.ok(
        auth.changePassword(CurrentOperator.id(), RevisionHeaders.parse(revision), key, body));
  }
}
