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
// HTTP 控制器：接收请求，返回值由框架序列化为响应，通常是 JSON。
@RestController
// 声明接口路径；类上的公共前缀会与方法上的路径拼接。
@RequestMapping("/internal/content/knowledge")
public class PublishedKnowledgeController {
  private final PublishedKnowledgeService content;
  private final String key;

  // 单一构造器由 Spring 自动注入依赖；final 字段表明这些依赖在构造后不再替换。
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

  // 将下面的方法映射为 GET 查询接口，不在此声明中处理数据库事务。
  @GetMapping
  public ApiResponse<Map<String, Object>> list(
      // 读取指定 HTTP 请求头，本项目用它传递幂等标识或并发版本等信息。
      @RequestHeader(value = "X-Content-Read-Key", required = false) String given,
      // 校验当前对象内部字段上的约束，必要时继续递归校验嵌套 DTO。
      @Valid @ModelAttribute AdminListQuery query) {
    requireKey(given);
    return ApiResponse.ok(content.list(query));
  }

  @GetMapping("/{uuid}")
  public ApiResponse<Map<String, Object>> detail(
      @RequestHeader(value = "X-Content-Read-Key", required = false) String given,
      // 读取 URL 路径中的变量，例如知识版本 UUID；不是从 JSON 请求体读取。
      @PathVariable String uuid) {
    requireKey(given);
    return ApiResponse.ok(content.detail(uuid));
  }
}
