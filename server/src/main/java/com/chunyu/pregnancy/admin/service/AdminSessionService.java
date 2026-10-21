package com.chunyu.pregnancy.admin.service;

import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.mapper.AdminAccessMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/** 过滤器只调用服务，不直接查Mapper；实时读取权限让角色调整下一次请求就生效。 */
// 业务层组件：由 Spring 管理，负责组织业务步骤；事务边界仍需由事务代码明确控制。
@Service
public class AdminSessionService {
  private final AdminAccessMapper access;

  // 单一构造器由 Spring 自动注入依赖；final 字段表明这些依赖在构造后不再替换。
  public AdminSessionService(AdminAccessMapper access) {
    this.access = access;
  }

  public List<String> permissions(Long actorId, String sessionUuid) {
    if (access.activeSession(actorId, sessionUuid) != 1) {
      throw new BizException(401, "AUTH_EXPIRED", "管理会话已失效");
    }
    return access.permissions(actorId);
  }
}
