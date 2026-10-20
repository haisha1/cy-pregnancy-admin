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
@RestController
@RequestMapping("/admin/api/v1")
public class AdminAuthController {
  private final AdminAuthService auth;
  private final AdminLoginRateLimiter limiter;

  public AdminAuthController(AdminAuthService auth, AdminLoginRateLimiter limiter) {
    this.auth = auth;
    this.limiter = limiter;
  }

  @PostMapping("/auth/login")
  @Operation(summary = "管理员密码登录，独立于小程序微信登录")
  public ApiResponse<TokenResponse> login(
      @Valid @RequestBody LoginRequest body, HttpServletRequest request) {
    limiter.check(request.getRemoteAddr(), body.username);
    return ApiResponse.ok(auth.login(body));
  }

  @GetMapping("/me")
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
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestHeader(value = "If-Match", required = false) String revision) {
    return ApiResponse.ok(
        auth.changePassword(CurrentOperator.id(), RevisionHeaders.parse(revision), key, body));
  }
}
