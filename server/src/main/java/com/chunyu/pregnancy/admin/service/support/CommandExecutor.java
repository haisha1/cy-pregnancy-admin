package com.chunyu.pregnancy.admin.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.config.AdminAuthProperties;
import com.chunyu.pregnancy.admin.dto.response.MutationResponse;
import com.chunyu.pregnancy.admin.entity.*;
import com.chunyu.pregnancy.admin.mapper.*;
import com.chunyu.pregnancy.admin.security.CurrentOperator;
import com.fasterxml.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 管理写入统一事务入口：锁定写窗口 → 复核当前权限 → 幂等比较 → 业务写入/审计 → 保存回执。 锁跨进程生效，避免多个Docker实例重复写。初期管理流量小，优先保持逻辑清晰。
 * 这里绝不保存请求全文，密码等请求只留下带服务端密钥的HMAC摘要。
 */
@Component
public class CommandExecutor {
  private final TransactionTemplate transactions;
  private final AdminAccessMapper access;
  private final AdminOperatorMapper operators;
  private final IdempotencyRecordMapper records;
  private final ObjectMapper json;
  private final AdminAuthProperties properties;

  public CommandExecutor(
      TransactionTemplate transactions,
      AdminAccessMapper access,
      AdminOperatorMapper operators,
      IdempotencyRecordMapper records,
      ObjectMapper json,
      AdminAuthProperties properties) {
    this.transactions = transactions;
    this.access = access;
    this.operators = operators;
    this.records = records;
    this.json = json.copy();
    this.json.setConfig(
        this.json.getSerializationConfig().with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
    this.json.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    this.properties = properties;
  }

  public MutationResponse execute(
      Long actorId,
      String permission,
      String scope,
      String operationUuid,
      Object payload,
      Supplier<MutationResponse> command) {
    validateUuid(operationUuid);
    String hash = fingerprint(payload);
    return transactions.execute(
        status -> {
          access.lockWrites();
          AdminOperator actor = operators.selectById(actorId);
          if (actor == null
              || !"active".equals(actor.getState())
              || access.activeSession(actorId, CurrentOperator.sessionUuid()) != 1) {
            throw new BizException(401, "AUTH_EXPIRED", "账号会话已失效");
          }
          if (permission != null && !access.permissions(actorId).contains(permission)) {
            throw new BizException(403, "FORBIDDEN", "没有执行此操作的权限");
          }
          IdempotencyRecord previous =
              records.selectOne(
                  new LambdaQueryWrapper<IdempotencyRecord>()
                      .eq(IdempotencyRecord::getActorId, actorId)
                      .eq(IdempotencyRecord::getCommandScope, scope)
                      .eq(IdempotencyRecord::getOperationUuid, operationUuid));
          if (previous != null) {
            if (!previous.getPayloadHash().equals(hash)) {
              throw new BizException(409, "IDEMPOTENCY_KEY_REUSED", "同一操作标识不能用于不同内容");
            }
            return decode(previous.getResponseJson());
          }
          MutationResponse response = command.get();
          IdempotencyRecord record = new IdempotencyRecord();
          record.setActorId(actorId);
          record.setCommandScope(scope);
          record.setOperationUuid(operationUuid);
          record.setPayloadHash(hash);
          record.setResponseJson(encode(response));
          record.setCreatedAt(Instant.now());
          records.insert(record);
          return response;
        });
  }

  public static void validateUuid(String uuid) {
    if (uuid == null
        || !uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
      throw new BizException(400, "UUID_INVALID", "资源及操作标识必须为小写UUID v4");
    }
  }

  public static void requireRevision(long current, long expected) {
    if (current != expected) {
      throw new BizException(409, "REVISION_CONFLICT", "资源已更新，请刷新后再操作");
    }
  }

  private String fingerprint(Object payload) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(
          new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] hash = mac.doFinal(json.writeValueAsBytes(payload));
      StringBuilder output = new StringBuilder(64);
      for (byte value : hash) {
        output.append(String.format("%02x", value & 0xff));
      }
      return output.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("无法计算操作摘要");
    }
  }

  private String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception ex) {
      throw new IllegalStateException("无法序列化操作回执");
    }
  }

  private MutationResponse decode(String value) {
    try {
      JsonNode tree = json.readTree(value);
      return new MutationResponse(
          tree.get("uuid").asText(),
          tree.get("revision").asLong(),
          tree.get("state").asText(),
          tree.get("parentUuid").isNull() ? null : tree.get("parentUuid").asText());
    } catch (Exception ex) {
      throw new IllegalStateException("无法恢复操作回执");
    }
  }
}
