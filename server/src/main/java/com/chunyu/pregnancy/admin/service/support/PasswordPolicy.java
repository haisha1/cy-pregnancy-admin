package com.chunyu.pregnancy.admin.service.support;

import com.chunyu.pregnancy.admin.common.exception.BizException;
import java.nio.charset.StandardCharsets;

/** BCrypt有效输入有字节上限，不能只按Java字符数判断而默默截断Unicode密码。 */
public final class PasswordPolicy {
  private PasswordPolicy() {}

  public static void validate(String password) {
    if (password == null
        || password.length() < 12
        || password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new BizException(422, "PASSWORD_INVALID", "密码至少12个字符且UTF-8长度不超过72字节");
    }
  }
}
