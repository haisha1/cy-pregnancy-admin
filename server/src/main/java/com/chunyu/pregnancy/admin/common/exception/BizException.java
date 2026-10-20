package com.chunyu.pregnancy.admin.common.exception;

import lombok.Getter;

/** 可预期的业务失败；message 只允许写可公开提示，不能包含凭据、SQL 或微信原始响应。 */
@Getter
public class BizException extends RuntimeException {
  private final int status;
  private final String code;

  public BizException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}
