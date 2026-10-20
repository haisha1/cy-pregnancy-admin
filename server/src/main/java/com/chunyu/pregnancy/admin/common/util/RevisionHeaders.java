package com.chunyu.pregnancy.admin.common.util;

import com.chunyu.pregnancy.admin.common.exception.BizException;

/** If-Match使用双引号包裹的整数版本，例如"3"；缺失和冲突是不同错误。 */
public final class RevisionHeaders {
  private RevisionHeaders() {}

  public static long parse(String value) {
    if (value == null) {
      throw new BizException(428, "REVISION_REQUIRED", "请提供If-Match版本");
    }
    if (!value.matches("\"[1-9][0-9]*\"")) {
      throw new BizException(400, "REVISION_INVALID", "If-Match格式应为双引号包裹的正整数");
    }
    try {
      return Long.parseLong(value.substring(1, value.length() - 1));
    } catch (NumberFormatException ex) {
      throw new BizException(400, "REVISION_INVALID", "资源版本超出允许范围");
    }
  }
}
