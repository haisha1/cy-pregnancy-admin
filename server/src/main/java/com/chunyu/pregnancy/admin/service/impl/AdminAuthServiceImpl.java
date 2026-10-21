package com.chunyu.pregnancy.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.config.AdminAuthProperties;
import com.chunyu.pregnancy.admin.dto.request.*;
import com.chunyu.pregnancy.admin.dto.response.*;
import com.chunyu.pregnancy.admin.entity.*;
import com.chunyu.pregnancy.admin.mapper.*;
import com.chunyu.pregnancy.admin.security.AdminTokenService;
import com.chunyu.pregnancy.admin.service.AdminAuthService;
import com.chunyu.pregnancy.admin.service.support.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 密码校验、可撤销会话和本人改密。密码散列计算放在写锁之前，减少全局锁持有时间。 */
// 业务层组件：由 Spring 管理，负责组织业务步骤；事务边界仍需由事务代码明确控制。
@Service
public class AdminAuthServiceImpl implements AdminAuthService {
  private final AdminOperatorMapper operators;
  private final AdminSessionMapper sessions;
  private final AdminAccessMapper access;
  private final PasswordEncoder passwords;
  private final AdminTokenService tokens;
  private final AdminAuthProperties properties;
  private final TransactionTemplate transactions;
  private final CommandExecutor commands;
  private final AuditWriter audit;
  private final String dummyHash;

  // 单一构造器由 Spring 自动注入依赖；final 字段表明这些依赖在构造后不再替换。
  public AdminAuthServiceImpl(
      AdminOperatorMapper operators,
      AdminSessionMapper sessions,
      AdminAccessMapper access,
      PasswordEncoder passwords,
      AdminTokenService tokens,
      AdminAuthProperties properties,
      TransactionTemplate transactions,
      CommandExecutor commands,
      AuditWriter audit) {
    this.operators = operators;
    this.sessions = sessions;
    this.access = access;
    this.passwords = passwords;
    this.tokens = tokens;
    this.properties = properties;
    this.transactions = transactions;
    this.commands = commands;
    this.audit = audit;
    this.dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  @Override
  // 先做 BCrypt 密码匹配，再持锁复核账号状态和密码是否变化，最后建立管理会话并记录审计。
  public TokenResponse login(LoginRequest request) {
    AdminOperator found =
        operators.selectOne(
            new LambdaQueryWrapper<AdminOperator>()
                .eq(AdminOperator::getUsername, request.username));
    // 不存在的账号也执行BCrypt，响应不透露用户名是否存在。
    boolean matched =
        passwords.matches(request.password, found == null ? dummyHash : found.getPasswordHash());
    if (!matched || found == null || !"active".equals(found.getState())) {
      audit.record(null, "auth", null, "login_failed", "AUTH_INVALID");
      throw new BizException(401, "AUTH_INVALID", "账号或密码无效");
    }
    return transactions.execute(
        status -> {
          access.lockWrites();
          // 密码计算在事务外；持锁后重新查一次，防止计算期间账号已被禁用或密码已被重置。
          AdminOperator current = operators.selectById(found.getId());
          if (!"active".equals(current.getState())
              || !current.getPasswordHash().equals(found.getPasswordHash())) {
            throw new BizException(401, "AUTH_INVALID", "账号或密码无效");
          }
          AdminSession session = new AdminSession();
          session.setClientUuid(UUID.randomUUID().toString());
          session.setOperatorId(current.getId());
          session.setCreatedAt(Instant.now());
          session.setExpiresAt(Instant.now().plusSeconds(properties.getAccessSeconds()));
          sessions.insert(session);
          audit.record(current.getId(), "operator", current.getClientUuid(), "login", "OK");
          return new TokenResponse(
              tokens.issue(current.getId(), session.getClientUuid()),
              "Bearer",
              properties.getAccessSeconds());
        });
  }

  @Override
  // 实时读取当前角色和权限，不将浏览器缓存的权限当成服务端授权依据。
  public MeResponse me(Long actorId) {
    AdminOperator operator = operators.selectById(actorId);
    if (operator == null || !"active".equals(operator.getState())) {
      throw new BizException(401, "AUTH_EXPIRED", "账号会话已失效");
    }
    return new MeResponse(
        new OperatorResponse(
            operator.getClientUuid(),
            operator.getUsername(),
            operator.getDisplayName(),
            operator.getState(),
            operator.getRevision(),
            access.roles(actorId)),
        access.permissions(actorId));
  }

  @Override
  public void logout(Long actorId, String sessionUuid) {
    transactions.executeWithoutResult(
        status -> {
          access.revokeSession(actorId, sessionUuid);
          audit.record(actorId, "session", sessionUuid, "logout", "OK");
        });
  }

  @Override
  // 验证新密码和旧密码、检查 revision，更新散列后撤销全部旧会话，要求重新登录。
  public MutationResponse changePassword(
      Long actorId, long expected, String operationUuid, ChangePasswordRequest request) {
    PasswordPolicy.validate(request.newPassword);
    String encoded = passwords.encode(request.newPassword);
    return commands.execute(
        actorId,
        null,
        "auth.password",
        operationUuid,
        new Object[] {expected, request},
        () -> {
          AdminOperator operator = operators.selectById(actorId);
          CommandExecutor.requireRevision(operator.getRevision(), expected);
          if (!passwords.matches(request.oldPassword, operator.getPasswordHash())) {
            throw new BizException(401, "AUTH_INVALID", "旧密码无效");
          }
          operator.setPasswordHash(encoded);
          operator.setRevision(operator.getRevision() + 1);
          operators.updateById(operator);
          access.revokeAll(actorId);
          audit.record(actorId, "operator", operator.getClientUuid(), "password_changed", "OK");
          return new MutationResponse(
              operator.getClientUuid(), operator.getRevision(), operator.getState(), null);
        });
  }
}
