package com.chunyu.pregnancy.admin.mapper;

import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.dto.request.AdminListQuery;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 只读查询适配层。SQL结构来自固定代码，所有用户输入使用占位参数，查询超时3秒。 */
@Repository
public class AdminReadMapper {
  private final JdbcTemplate db;

  public AdminReadMapper(DataSource source) {
    db = new JdbcTemplate(source);
    db.setQueryTimeout(3);
  }

  private static boolean has(String v) {
    return v != null && !v.trim().isEmpty();
  }

  private static String like(String v) {
    return "%" + v.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
  }

  public List<Map<String, Object>> knowledge(AdminListQuery q, String mode, Long actor) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT v.id cursorId,a.client_uuid articleUuid,a.slug,a.revision articleRevision,v.client_uuid uuid,v.version_no versionNo,v.title,v.category,v.state,v.revision,COALESCE(a.published_version_id=v.id,0) currentlyPublished,o.client_uuid authorUuid,o.display_name authorName,v.created_at createdAt,v.published_at publishedAt FROM admin_knowledge_version v JOIN admin_knowledge_article a ON a.id=v.article_id JOIN admin_operator o ON o.id=v.author_id WHERE 1=1");
    List<Object> p = new ArrayList<>();
    if ("latest".equals(mode))
      sql.append(
          " AND v.version_no=(SELECT MAX(x.version_no) FROM admin_knowledge_version x WHERE x.article_id=a.id)");
    if ("review".equals(mode)) {
      sql.append(" AND v.state='submitted' AND v.author_id<>?");
      p.add(actor);
    }
    if ("release".equals(mode)) sql.append(" AND v.state='approved'");
    if ("current".equals(mode)) sql.append(" AND a.published_version_id=v.id");
    if (has(q.getQ())) {
      sql.append(
          " AND (v.title LIKE ? ESCAPE '!' OR CONVERT(a.slug USING utf8mb4) LIKE ? ESCAPE '!')");
      p.add(like(q.getQ()));
      p.add(like(q.getQ()));
    }
    if (has(q.getCategory())) {
      sql.append(" AND v.category=?");
      p.add(q.getCategory());
    }
    if (has(q.getState())) {
      sql.append(" AND v.state=?");
      p.add(q.getState());
    }
    return finish(sql, p, q, "v.id", "v.created_at");
  }

  public List<Map<String, Object>> operators(AdminListQuery q) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT o.id cursorId,o.client_uuid uuid,o.username,o.display_name displayName,o.state,o.revision,o.created_at createdAt,(SELECT GROUP_CONCAT(role_code ORDER BY role_code) FROM admin_operator_role r WHERE r.operator_id=o.id) roles FROM admin_operator o WHERE 1=1");
    List<Object> p = new ArrayList<>();
    if (has(q.getQ())) {
      sql.append(
          " AND (CONVERT(o.username USING utf8mb4) LIKE ? ESCAPE '!' OR o.display_name LIKE ? ESCAPE '!')");
      p.add(like(q.getQ()));
      p.add(like(q.getQ()));
    }
    if (has(q.getState())) {
      sql.append(" AND o.state=?");
      p.add(q.getState());
    }
    if (has(q.getRole())) {
      sql.append(
          " AND EXISTS(SELECT 1 FROM admin_operator_role r WHERE r.operator_id=o.id AND r.role_code=?)");
      p.add(q.getRole());
    }
    List<Map<String, Object>> rows = finish(sql, p, q, "o.id", "o.created_at");
    for (Map<String, Object> r : rows) {
      Object roles = r.remove("roles");
      r.put(
          "roleCodes",
          roles == null ? Collections.emptyList() : Arrays.asList(roles.toString().split(",")));
    }
    return rows;
  }

  public List<Map<String, Object>> audits(AdminListQuery q) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT e.id cursorId,e.id,e.actor_id actorId,o.display_name actorName,e.resource_type resourceType,e.resource_uuid resourceUuid,e.action,e.result_code resultCode,e.request_id requestId,e.occurred_at occurredAt FROM admin_audit_event e LEFT JOIN admin_operator o ON o.id=e.actor_id WHERE 1=1");
    List<Object> p = new ArrayList<>();
    if (has(q.getQ())) {
      sql.append(
          " AND (o.display_name LIKE ? ESCAPE '!' OR CONVERT(e.resource_uuid USING utf8mb4) LIKE ? ESCAPE '!' OR CONVERT(e.request_id USING utf8mb4) LIKE ? ESCAPE '!')");
      for (int i = 0; i < 3; i++) p.add(like(q.getQ()));
    }
    if (has(q.getAction())) {
      sql.append(" AND e.action=?");
      p.add(q.getAction());
    }
    if (has(q.getResult())) {
      sql.append(" AND e.result_code=?");
      p.add(q.getResult());
    }
    return finish(sql, p, q, "e.id", "e.occurred_at");
  }

  private List<Map<String, Object>> finish(
      StringBuilder sql, List<Object> p, AdminListQuery q, String id, String time) {
    if (q.getLimit() < 1 || q.getLimit() > 100)
      throw new BizException(422, "PAGE_INVALID", "每批数量须为1–100");
    if (q.getBeforeId() != null) {
      sql.append(" AND " + id + "<?");
      p.add(q.getBeforeId());
    }
    try {
      LocalDate start = has(q.getStart()) ? LocalDate.parse(q.getStart()) : null,
          end = has(q.getEnd()) ? LocalDate.parse(q.getEnd()) : null;
      if (start != null && end != null && start.isAfter(end)) throw new IllegalArgumentException();
      if (start != null) {
        sql.append(" AND " + time + ">=?");
        p.add(Timestamp.from(start.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant()));
      }
      if (end != null) {
        sql.append(" AND " + time + "<?");
        p.add(Timestamp.from(end.plusDays(1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant()));
      }
    } catch (Exception e) {
      throw new BizException(422, "DATE_RANGE_INVALID", "请核对日期范围，开始日期不能晚于结束日期");
    }
    sql.append(" ORDER BY " + id + " DESC LIMIT ?");
    p.add(q.getLimit() + 1);
    return normalize(db.queryForList(sql.toString(), p.toArray()));
  }

  public List<Map<String, Object>> query(String sql, Object... p) {
    return normalize(db.queryForList(sql, p));
  }

  public long count(String sql, Object... p) {
    Long n = db.queryForObject(sql, Long.class, p);
    return n == null ? 0 : n;
  }

  private List<Map<String, Object>> normalize(List<Map<String, Object>> rows) {
    for (Map<String, Object> row : rows)
      for (Map.Entry<String, Object> e : row.entrySet()) {
        Object v = e.getValue();
        if ("currentlyPublished".equals(e.getKey()))
          e.setValue(
              v instanceof Boolean ? v : v instanceof Number && ((Number) v).intValue() != 0);
        else if (v instanceof Timestamp) e.setValue(((Timestamp) v).toInstant());
        else if (v instanceof LocalDateTime)
          e.setValue(((LocalDateTime) v).toInstant(ZoneOffset.UTC));
      }
    return rows;
  }
}
