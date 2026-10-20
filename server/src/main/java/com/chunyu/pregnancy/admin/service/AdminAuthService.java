package com.chunyu.pregnancy.admin.service;

import com.chunyu.pregnancy.admin.dto.request.*;
import com.chunyu.pregnancy.admin.dto.response.*;

/** 管理端账号业务契约，与小程序微信身份完全独立。 */
public interface AdminAuthService {
  TokenResponse login(LoginRequest request);

  MeResponse me(Long actorId);

  void logout(Long actorId, String sessionUuid);

  MutationResponse changePassword(
      Long actorId, long expected, String operationUuid, ChangePasswordRequest request);
}
