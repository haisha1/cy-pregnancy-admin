package com.chunyu.pregnancy.admin.service.impl;

import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.dto.request.AdminListQuery;
import com.chunyu.pregnancy.admin.dto.response.PageResponse;
import com.chunyu.pregnancy.admin.mapper.*;
import com.chunyu.pregnancy.admin.service.AdminReadService;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** 最小用户数据经内部HTTP读取，不跨库读取健康数据。任何外部服务失败均显式返回不可用。 */
@Service
public class AdminReadServiceImpl implements AdminReadService {
  private final AdminReadMapper data;
  private final AdminAccessMapper access;
  private final StringRedisTemplate redis;
  private final RestTemplate http;

  @Value("${app.user-read.url:http://127.0.0.1:18731}")
  private String userUrl;

  @Value("${app.user-read.key:}")
  private String userKey;

  public AdminReadServiceImpl(
      AdminReadMapper data, AdminAccessMapper access, StringRedisTemplate redis) {
    this.data = data;
    this.access = access;
    this.redis = redis;
    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(1500);
    f.setReadTimeout(2500);
    http = new RestTemplate(f);
  }

  private PageResponse<Map<String, Object>> page(List<Map<String, Object>> rows, int limit) {
    boolean more = rows.size() > limit;
    if (more) rows = new ArrayList<>(rows.subList(0, limit));
    Long cursor = more ? ((Number) rows.get(rows.size() - 1).get("cursorId")).longValue() : null;
    for (Map<String, Object> r : rows) r.remove("cursorId");
    return new PageResponse<>(rows, cursor);
  }

  public PageResponse<Map<String, Object>> knowledge(AdminListQuery q, String mode, Long actor) {
    return page(data.knowledge(q, mode, actor), q.getLimit());
  }

  public PageResponse<Map<String, Object>> operators(AdminListQuery q) {
    return page(data.operators(q), q.getLimit());
  }

  public Map<String, Object> operator(String uuid) {
    AdminListQuery q = new AdminListQuery();
    q.setLimit(1);
    List<Map<String, Object>> rows =
        data.query(
            "SELECT id cursorId,client_uuid uuid,username,display_name displayName,state,revision,created_at createdAt FROM admin_operator WHERE client_uuid=?",
            uuid);
    if (rows.isEmpty()) throw new BizException(404, "RESOURCE_NOT_FOUND", "管理账号不存在");
    Map<String, Object> row = rows.get(0);
    Long id = ((Number) row.remove("cursorId")).longValue();
    row.put("roleCodes", access.roles(id));
    return row;
  }

  public PageResponse<Map<String, Object>> audits(AdminListQuery q) {
    return page(data.audits(q), q.getLimit());
  }

  public Map<String, Object> summary(Long actor) {
    Map<String, Object> out = new LinkedHashMap<>();
    List<String> perms = access.permissions(actor);
    out.put("updatedAt", Instant.now());
    if (perms.contains("knowledge:read")) {
      out.put("articles", data.count("SELECT COUNT(*) FROM admin_knowledge_article"));
      out.put(
          "published",
          data.count(
              "SELECT COUNT(*) FROM admin_knowledge_article WHERE published_version_id IS NOT NULL"));
      out.put(
          "pendingReview",
          data.count(
              "SELECT COUNT(*) FROM admin_knowledge_version WHERE state='submitted' AND author_id<>?",
              actor));
      out.put(
          "pendingRelease",
          data.count("SELECT COUNT(*) FROM admin_knowledge_version WHERE state='approved'"));
      out.put(
          "myDrafts",
          data.count(
              "SELECT COUNT(*) FROM admin_knowledge_version WHERE state='draft' AND author_id=?",
              actor));
    }
    if (perms.contains("user:read")) {
      try {
        out.put("users", userStats(7));
        out.put("userStatus", "available");
      } catch (BizException e) {
        out.put("users", null);
        out.put("userStatus", "unavailable");
      }
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> bridge(String path, Map<String, Object> params) {
    if (userKey.length() < 32) throw new BizException(503, "USER_READ_UNAVAILABLE", "用户查询服务尚未配置");
    UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(userUrl).path(path);
    params.forEach(
        (k, v) -> {
          if (v != null && !v.toString().isEmpty()) uri.queryParam(k, v);
        });
    HttpHeaders h = new HttpHeaders();
    h.set("X-Admin-Read-Key", userKey);
    try {
      Map<String, Object> response =
          http.exchange(
                  uri.build().encode().toUri(), HttpMethod.GET, new HttpEntity<>(h), Map.class)
              .getBody();
      if (response == null
          || !"OK".equals(response.get("code"))
          || !(response.get("data") instanceof Map)) throw new IllegalStateException();
      return (Map<String, Object>) response.get("data");
    } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
      throw new BizException(404, "RESOURCE_NOT_FOUND", "用户不存在");
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      if (e.getRawStatusCode() == 400 || e.getRawStatusCode() == 422)
        throw new BizException(422, "QUERY_INVALID", "用户查询条件无效，请检查状态或日期范围");
      throw new BizException(503, "USER_READ_UNAVAILABLE", "用户查询暂不可用，请稍后重试");
    } catch (Exception e) {
      throw new BizException(503, "USER_READ_UNAVAILABLE", "用户查询暂不可用，请稍后重试");
    }
  }

  public Map<String, Object> users(AdminListQuery q) {
    Map<String, Object> p = new LinkedHashMap<>();
    p.put("q", q.getQ());
    p.put("state", q.getState());
    p.put("beforeId", q.getBeforeId());
    p.put("limit", q.getLimit());
    p.put("start", q.getStart());
    p.put("end", q.getEnd());
    return bridge("/internal/admin/users", p);
  }

  public Map<String, Object> user(String uuid) {
    try {
      java.util.UUID.fromString(uuid);
    } catch (Exception e) {
      throw new BizException(422, "UUID_INVALID", "用户标识无效");
    }
    return bridge("/internal/admin/users/" + uuid, Collections.emptyMap());
  }

  public Map<String, Object> userStats(int days) {
    return bridge("/internal/admin/user-stats", Collections.singletonMap("days", days));
  }

  public Map<String, Object> health() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("checkedAt", Instant.now());
    try {
      out.put("database", data.count("SELECT 1") == 1 ? "up" : "down");
    } catch (Exception e) {
      out.put("database", "down");
    }
    try {
      String pong =
          redis.execute((org.springframework.data.redis.core.RedisCallback<String>) c -> c.ping());
      out.put("redis", "PONG".equals(pong) ? "up" : "down");
    } catch (Exception e) {
      out.put("redis", "down");
    }
    try {
      userStats(7);
      out.put("userService", "up");
    } catch (Exception e) {
      out.put("userService", "unavailable");
    }
    try {
      Map<?, ?> content =
          http.getForObject(userUrl.replaceAll("/+$", "") + "/api/v1/knowledge?limit=1", Map.class);
      out.put(
          "contentDelivery",
          content != null && "OK".equals(content.get("code")) ? "up" : "unavailable");
    } catch (Exception e) {
      out.put("contentDelivery", "unavailable");
    }
    return out;
  }
}
