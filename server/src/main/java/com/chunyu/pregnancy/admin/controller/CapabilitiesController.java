package com.chunyu.pregnancy.admin.controller;

import com.chunyu.pregnancy.admin.common.result.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.*;
import org.springframework.web.bind.annotation.*;

/** 真实能力状态，不把计划中的模型、工单、任务管理显示成已接通。 */
// HTTP 控制器：接收请求，返回值由框架序列化为响应，通常是 JSON。
@RestController
// 声明 OpenAPI 文档中的认证要求，不会代替 Spring Security 的实际拦截。
@SecurityRequirement(name = "adminBearer")
public class CapabilitiesController {
  // 将下面的方法映射为 GET 查询接口，不在此声明中处理数据库事务。
  @GetMapping("/admin/api/v1/capabilities")
  public ApiResponse<Map<String, Boolean>> capabilities() {
    Map<String, Boolean> capabilities = new LinkedHashMap<>();
    for (String key :
        new String[] {
          "auth",
          "operators",
          "roles",
          "audit",
          "knowledgeWorkflow",
          "dashboard",
          "userRead",
          "knowledgeSearch",
          "reviewQueue",
          "releaseQueue",
          "miniappContentDelivery"
        }) {
      capabilities.put(key, true);
    }
    for (String key :
        new String[] {"modelRelease", "feedback", "jobs", "deletions", "mfa", "releaseRollback"}) {
      capabilities.put(key, false);
    }
    return ApiResponse.ok(capabilities);
  }
}
