package com.chunyu.pregnancy.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.dto.request.*;
import com.chunyu.pregnancy.admin.dto.response.*;
import com.chunyu.pregnancy.admin.entity.AdminOperator;
import com.chunyu.pregnancy.admin.mapper.*;
import com.chunyu.pregnancy.admin.service.AdminOperatorService;
import com.chunyu.pregnancy.admin.service.support.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** 账号业务实现：角色白名单、乐观锁和最后一名安全管理员保护均在事务写锁内执行。 */
// 业务层组件：由 Spring 管理，负责组织业务步骤；事务边界仍需由事务代码明确控制。
@Service
public class AdminOperatorServiceImpl implements AdminOperatorService {
  private final AdminOperatorMapper operators;
  private final AdminAccessMapper access;
  private final CommandExecutor commands;
  private final PasswordEncoder passwords;
  private final AuditWriter audit;

  // 单一构造器由 Spring 自动注入依赖；final 字段表明这些依赖在构造后不再替换。
  public AdminOperatorServiceImpl(
      AdminOperatorMapper operators,
      AdminAccessMapper access,
      CommandExecutor commands,
      PasswordEncoder passwords,
      AuditWriter audit) {
    this.operators = operators;
    this.access = access;
    this.commands = commands;
    this.passwords = passwords;
    this.audit = audit;
  }

  @Override
  public PageResponse<OperatorResponse> list(Long beforeId, int limit) {
    List<AdminOperator> rows =
        operators.selectList(
            new LambdaQueryWrapper<AdminOperator>()
                .lt(beforeId != null, AdminOperator::getId, beforeId)
                .orderByDesc(AdminOperator::getId)
                .last("LIMIT " + (bounded(limit) + 1)));
    boolean more = rows.size() > limit;
    if (more) {
      rows = rows.subList(0, limit);
    }
    Long cursor = more ? rows.get(rows.size() - 1).getId() : null;
    return new PageResponse<>(
        rows.stream().map(this::response).collect(Collectors.toList()), cursor);
  }

  @Override
  public List<RoleResponse> roles() {
    List<RoleResponse> roles = access.allRoles();
    for (RoleResponse role : roles) {
      role.setPermissions(access.rolePermissions(role.getCode()));
    }
    return roles;
  }

  @Override
  // 校验角色白名单和密码规则，生成 BCrypt 散列，再在统一写事务中创建账号及角色关联。
  public MutationResponse create(Long actorId, String key, CreateOperatorRequest request) {
    CommandExecutor.validateUuid(request.clientUuid);
    PasswordPolicy.validate(request.password);
    String encoded = passwords.encode(request.password);
    return commands.execute(
        actorId,
        "operator:manage",
        "operator.create",
        key,
        request,
        () -> {
          validateRoles(request.roleCodes);
          if (operators.selectCount(
                  new LambdaQueryWrapper<AdminOperator>()
                      .eq(AdminOperator::getUsername, request.username)
                      .or()
                      .eq(AdminOperator::getClientUuid, request.clientUuid))
              > 0) {
            throw new BizException(409, "OPERATOR_EXISTS", "账号名称或资源标识已存在");
          }
          AdminOperator operator = new AdminOperator();
          operator.setClientUuid(request.clientUuid);
          operator.setUsername(request.username);
          operator.setDisplayName(request.displayName);
          operator.setPasswordHash(encoded);
          operator.setState("active");
          operator.setRevision(1L);
          operator.setCreatedAt(Instant.now());
          operators.insert(operator);
          assignRoles(operator.getId(), request.roleCodes);
          audit.record(actorId, "operator", operator.getClientUuid(), "operator_created", "OK");
          return mutation(operator);
        });
  }

  @Override
  // 修改状态/角色前保护最后一名可用安全管理员；成功后撤销目标账号旧会话。
  public MutationResponse update(
      Long actorId, String uuid, long expected, String key, UpdateOperatorRequest request) {
    return commands.execute(
        actorId,
        "operator:manage",
        "operator.update",
        key,
        new Object[] {uuid, expected, request},
        () -> {
          validateRoles(request.roleCodes);
          AdminOperator operator = find(uuid);
          CommandExecutor.requireRevision(operator.getRevision(), expected);
          boolean wasAdmin =
              "active".equals(operator.getState())
                  && access.roles(operator.getId()).contains("security_admin");
          boolean remainsAdmin =
              "active".equals(request.state) && request.roleCodes.contains("security_admin");
          if (wasAdmin && !remainsAdmin && access.activeSecurityAdmins() <= 1) {
            throw new BizException(409, "LAST_SECURITY_ADMIN", "至少保留一名可用安全管理员");
          }
          operator.setDisplayName(request.displayName);
          operator.setState(request.state);
          operator.setRevision(operator.getRevision() + 1);
          operators.updateById(operator);
          assignRoles(operator.getId(), request.roleCodes);
          // 状态/角色变化撤销全部会话，要求使用新权限重新登录；每次请求仍读取实时权限。
          access.revokeAll(operator.getId());
          audit.record(actorId, "operator", uuid, "operator_updated", "OK");
          return mutation(operator);
        });
  }

  @Override
  // 管理员重置目标密码，不需要知道原密码；更新后撤销目标账号全部会话。
  public MutationResponse resetPassword(
      Long actorId, String uuid, long expected, String key, ResetPasswordRequest request) {
    PasswordPolicy.validate(request.password);
    String encoded = passwords.encode(request.password);
    return commands.execute(
        actorId,
        "operator:manage",
        "operator.password",
        key,
        new Object[] {uuid, expected, request},
        () -> {
          AdminOperator operator = find(uuid);
          CommandExecutor.requireRevision(operator.getRevision(), expected);
          operator.setPasswordHash(encoded);
          operator.setRevision(operator.getRevision() + 1);
          operators.updateById(operator);
          access.revokeAll(operator.getId());
          audit.record(actorId, "operator", uuid, "password_reset", "OK");
          return mutation(operator);
        });
  }

  private void validateRoles(List<String> roles) {
    if (roles == null
        || roles.isEmpty()
        || new HashSet<>(roles).size() != roles.size()
        || roles.stream().anyMatch(role -> access.roleExists(role) != 1)) {
      throw new BizException(422, "ROLE_INVALID", "角色必须来自预置清单且不可重复");
    }
  }

  private void assignRoles(Long id, List<String> roles) {
    access.clearRoles(id);
    for (String role : roles) {
      access.assignRole(id, role);
    }
  }

  private AdminOperator find(String uuid) {
    AdminOperator operator =
        operators.selectOne(
            new LambdaQueryWrapper<AdminOperator>().eq(AdminOperator::getClientUuid, uuid));
    if (operator == null) {
      throw new BizException(404, "RESOURCE_NOT_FOUND", "管理账号不存在");
    }
    return operator;
  }

  private OperatorResponse response(AdminOperator operator) {
    return new OperatorResponse(
        operator.getClientUuid(),
        operator.getUsername(),
        operator.getDisplayName(),
        operator.getState(),
        operator.getRevision(),
        access.roles(operator.getId()));
  }

  private MutationResponse mutation(AdminOperator operator) {
    return new MutationResponse(
        operator.getClientUuid(), operator.getRevision(), operator.getState(), null);
  }

  private int bounded(int limit) {
    if (limit < 1 || limit > 100) {
      throw new BizException(422, "PAGE_INVALID", "每页数量必须为1–100");
    }
    return limit;
  }
}
