package com.chunyu.pregnancy.admin.controller;

import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.common.result.ApiResponse;
import com.chunyu.pregnancy.admin.dto.request.AdminListQuery;
import com.chunyu.pregnancy.admin.service.PublishedKnowledgeService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/** 仅供用户服务读取已发布内容；管理入口不直接向小程序开放。独立密钥不能使用JWT替代。 */
@RestController
@RequestMapping("/internal/content/knowledge")
public class PublishedKnowledgeController {
  private final PublishedKnowledgeService content;
  private final String key;

  public PublishedKnowledgeController(
      PublishedKnowledgeService content, @Value("${CONTENT_READ_KEY:}") String key) {
    this.content = content;
    this.key = key;
  }

  private void requireKey(String given) {
    if (key.length() < 32) throw new BizException(503, "CONTENT_NOT_CONFIGURED", "内容读取未配置");
    if (given == null
        || !MessageDigest.isEqual(
            key.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8)))
      throw new BizException(403, "FORBIDDEN", "内容读取凭据无效");
  }

  @GetMapping
  public ApiResponse<Map<String, Object>> list(
      @RequestHeader(value = "X-Content-Read-Key", required = false) String given,
      @Valid @ModelAttribute AdminListQuery query) {
    requireKey(given);
    return ApiResponse.ok(content.list(query));
  }

  @GetMapping("/{uuid}")
  public ApiResponse<Map<String, Object>> detail(
      @RequestHeader(value = "X-Content-Read-Key", required = false) String given,
      @PathVariable String uuid) {
    requireKey(given);
    return ApiResponse.ok(content.detail(uuid));
  }
}
