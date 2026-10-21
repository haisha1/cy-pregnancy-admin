package com.chunyu.pregnancy.admin.service;

import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.dto.request.AdminListQuery;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 已发布只读投影：只跟随article的当前指针，不按客户端给的版本号读取历史。下架立即不可读，不缓存正文。 */
// 业务层组件：由 Spring 管理，负责组织业务步骤；事务边界仍需由事务代码明确控制。
@Service
public class PublishedKnowledgeService {
  private final JdbcTemplate db;
  private final ObjectMapper json;
  private static final String FROM =
      " FROM admin_knowledge_article a JOIN admin_knowledge_version v ON v.id=a.published_version_id WHERE v.state='published'";
  private static final String FIELDS =
      "a.id cursorId,a.client_uuid uuid,a.slug,v.client_uuid versionUuid,v.version_no versionNo,v.title,v.category,v.published_at publishedAt";

  // 单一构造器由 Spring 自动注入依赖；final 字段表明这些依赖在构造后不再替换。
  public PublishedKnowledgeService(DataSource source, ObjectMapper json) {
    db = new JdbcTemplate(source);
    db.setQueryTimeout(3);
    this.json = json;
  }

  public Map<String, Object> list(AdminListQuery query) {
    String sql = "SELECT " + FIELDS + FROM;
    List<Object> values = new ArrayList<>();
    if (query.getQ() != null && !query.getQ().trim().isEmpty()) {
      sql += " AND v.title LIKE ? ESCAPE '!'";
      values.add(
          "%" + query.getQ().trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
    }
    if (query.getCategory() != null && !query.getCategory().isEmpty()) {
      sql += " AND v.category=?";
      values.add(query.getCategory());
    }
    if (query.getBeforeId() != null) {
      sql += " AND a.id<?";
      values.add(query.getBeforeId());
    }
    sql += " ORDER BY a.id DESC LIMIT ?";
    values.add(query.getLimit() + 1);
    List<Map<String, Object>> rows = db.queryForList(sql, values.toArray());
    boolean more = rows.size() > query.getLimit();
    if (more) rows = new ArrayList<>(rows.subList(0, query.getLimit()));
    Long next = more ? ((Number) rows.get(rows.size() - 1).get("cursorId")).longValue() : null;
    rows.forEach(this::normalize);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("records", rows);
    result.put("nextBeforeId", next);
    return result;
  }

  public Map<String, Object> detail(String uuid) {
    if (!uuid.matches("[0-9a-fA-F-]{36}")) throw missing();
    List<Map<String, Object>> rows =
        db.queryForList(
            "SELECT "
                + FIELDS
                + ",v.pro_content proContent,v.plain_content plainContent,v.references_json referencesJson,v.evidence_note evidenceNote"
                + FROM
                + " AND a.client_uuid=?",
            uuid);
    if (rows.isEmpty()) throw missing();
    Map<String, Object> row = rows.get(0);
    normalize(row);
    try {
      row.put(
          "references",
          json.readValue(
              row.remove("referencesJson").toString(), new TypeReference<List<String>>() {}));
    } catch (Exception ex) {
      throw new BizException(503, "CONTENT_INVALID", "已发布内容暂不可用");
    }
    return row;
  }

  private void normalize(Map<String, Object> row) {
    row.remove("cursorId");
    Object time = row.get("publishedAt");
    if (time instanceof java.time.LocalDateTime)
      row.put("publishedAt", ((java.time.LocalDateTime) time).toInstant(java.time.ZoneOffset.UTC));
    if (time instanceof Timestamp)
      row.put(
          "publishedAt", ((Timestamp) time).toLocalDateTime().toInstant(java.time.ZoneOffset.UTC));
  }

  private BizException missing() {
    return new BizException(404, "CONTENT_NOT_FOUND", "内容未发布或已下架");
  }
}
