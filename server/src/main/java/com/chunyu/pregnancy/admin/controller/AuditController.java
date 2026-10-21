package com.chunyu.pregnancy.admin.controller;

import com.chunyu.pregnancy.admin.common.result.ApiResponse;
import com.chunyu.pregnancy.admin.dto.response.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import javax.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 审计只读接口，单独权限，与内容查看权限分离。 */
// HTTP 控制器：接收请求，返回值由框架序列化为响应，通常是 JSON。
@RestController
// 启用此处的参数约束校验；配置类可校验配置值，Controller 可校验方法参数。
@Validated
// 声明 OpenAPI 文档中的认证要求，不会代替 Spring Security 的实际拦截。
@SecurityRequirement(name = "adminBearer")
// 声明接口路径；类上的公共前缀会与方法上的路径拼接。
@RequestMapping("/admin/api/v1/audits")
public class AuditController {
  private final com.chunyu.pregnancy.admin.service.AdminReadService audit;

  // 单一构造器由 Spring 自动注入依赖；final 字段表明这些依赖在构造后不再替换。
  public AuditController(com.chunyu.pregnancy.admin.service.AdminReadService audit) {
    this.audit = audit;
  }

  // 将下面的方法映射为 GET 查询接口，不在此声明中处理数据库事务。
  @GetMapping
  // 方法执行前检查当前认证信息中的权限；隐藏前端按钮不能替代这里的服务端检查。
  @PreAuthorize("hasAuthority('audit:read')")
  public ApiResponse<PageResponse<java.util.Map<String, Object>>> list(
      @javax.validation.Valid @ModelAttribute
          com.chunyu.pregnancy.admin.dto.request.AdminListQuery query) {
    return ApiResponse.ok(audit.audits(query));
  }
}
