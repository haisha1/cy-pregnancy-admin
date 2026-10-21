package com.chunyu.pregnancy.admin.config;

import com.chunyu.pregnancy.admin.entity.AdminOperator;
import com.chunyu.pregnancy.admin.mapper.*;
import com.chunyu.pregnancy.admin.service.support.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** 仅在显式启用且账号表完全为空时创建首位安全管理员。没有固定密码，不打印密码。 账号表非空后不更新密码或角色，重启不能变成恢复被禁用账号的后门。 */
// 通用 Spring 组件：由容器创建并管理，其他类可通过构造器使用它。
@Component
public class AdminBootstrap implements ApplicationRunner {
  private final AdminAuthProperties properties;
  private final AdminOperatorMapper operators;
  private final AdminAccessMapper access;
  private final PasswordEncoder passwords;
  private final TransactionTemplate transactions;
  private final AuditWriter audit;

  public AdminBootstrap(
      AdminAuthProperties properties,
      AdminOperatorMapper operators,
      AdminAccessMapper access,
      PasswordEncoder passwords,
      TransactionTemplate transactions,
      AuditWriter audit) {
    this.properties = properties;
    this.operators = operators;
    this.access = access;
    this.passwords = passwords;
    this.transactions = transactions;
    this.audit = audit;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    if (!properties.isBootstrapEnabled()) {
      return;
    }
    transactions.executeWithoutResult(
        status -> {
          access.lockWrites();
          // 仅用于首次空库引导：已有任意账号就跳过，不因重启而重置密码或提升角色。
          if (operators.selectCount(null) != 0) {
            return;
          }
          PasswordPolicy.validate(properties.getBootstrapPassword());
          if (properties.getBootstrapUsername() == null
              || !properties.getBootstrapUsername().matches("[a-z][a-z0-9_]{2,39}")) {
            throw new IllegalStateException("初始管理用户名格式无效");
          }
          AdminOperator operator = new AdminOperator();
          operator.setClientUuid(UUID.randomUUID().toString());
          operator.setUsername(properties.getBootstrapUsername());
          operator.setPasswordHash(passwords.encode(properties.getBootstrapPassword()));
          operator.setDisplayName("初始安全管理员");
          operator.setState("active");
          operator.setRevision(1L);
          operator.setCreatedAt(Instant.now());
          operators.insert(operator);
          access.assignRole(operator.getId(), "security_admin");
          audit.record(
              operator.getId(), "operator", operator.getClientUuid(), "bootstrap_created", "OK");
        });
  }
}
