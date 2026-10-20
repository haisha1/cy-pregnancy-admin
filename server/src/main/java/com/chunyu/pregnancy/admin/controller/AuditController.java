package com.chunyu.pregnancy.admin.controller;

import com.chunyu.pregnancy.admin.common.result.ApiResponse;
import com.chunyu.pregnancy.admin.dto.response.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import javax.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 审计只读接口，单独权限，与内容查看权限分离。 */
@RestController
@Validated
@SecurityRequirement(name = "adminBearer")
@RequestMapping("/admin/api/v1/audits")
public class AuditController {
  private final com.chunyu.pregnancy.admin.service.AdminReadService audit;

  public AuditController(com.chunyu.pregnancy.admin.service.AdminReadService audit) {
    this.audit = audit;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('audit:read')")
  public ApiResponse<PageResponse<java.util.Map<String, Object>>> list(
      @javax.validation.Valid @ModelAttribute
          com.chunyu.pregnancy.admin.dto.request.AdminListQuery query) {
    return ApiResponse.ok(audit.audits(query));
  }
}
