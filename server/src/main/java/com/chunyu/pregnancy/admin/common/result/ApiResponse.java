package com.chunyu.pregnancy.admin.common.result;

import java.time.Instant;
import lombok.Getter;
import org.slf4j.MDC;

/** 所有业务接口的统一响应。requestId 用来排查问题，不需要在日志中记录健康请求体。 */
@Getter
public class ApiResponse<T> {
  private final String code;
  private final String message;
  private final T data;
  private final String requestId;
  private final Instant serverTime = Instant.now();

  private ApiResponse(String code, String message, T data) {
    this.code = code;
    this.message = message;
    this.data = data;
    this.requestId = MDC.get("requestId");
  }

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>("OK", "ok", data);
  }

  public static ApiResponse<Void> error(String code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}
