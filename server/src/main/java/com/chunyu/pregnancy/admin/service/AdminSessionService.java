package com.chunyu.pregnancy.admin.service;

import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.mapper.AdminAccessMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/** 过滤器只调用服务，不直接查Mapper；实时读取权限让角色调整下一次请求就生效。 */
@Service
public class AdminSessionService {
  private final AdminAccessMapper access;

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
