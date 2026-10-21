package com.chunyu.pregnancy.admin.common.exception;

import com.chunyu.pregnancy.admin.common.result.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** MVC 层错误处理；Spring Security 过滤器层的错误在 SecurityConfig 单独输出。 */
// 集中处理 Controller 抛出的异常并生成统一响应；安全过滤器异常另行处理。
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // 指定此方法处理的异常类型，将预期失败转换成对应 HTTP 状态和错误包体。
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> forbidden(
      org.springframework.security.access.AccessDeniedException ex) {
    return ResponseEntity.status(403).body(ApiResponse.error("FORBIDDEN", "没有执行此操作的权限"));
  }

  @ExceptionHandler(javax.validation.ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> constraint(
      javax.validation.ConstraintViolationException ex) {
    return ResponseEntity.unprocessableEntity()
        .body(ApiResponse.error("VALIDATION_FAILED", "参数超出允许范围"));
  }

  @ExceptionHandler(BizException.class)
  public ResponseEntity<ApiResponse<Void>> business(BizException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException ex) {
    return ResponseEntity.unprocessableEntity()
        .body(ApiResponse.error("VALIDATION_FAILED", "请求字段缺失或格式无效"));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> malformed(HttpMessageNotReadableException ex) {
    return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "JSON格式或字段无效"));
  }

  /** @ModelAttribute查询参数校验也必须输出422，不能被兜底处理误判为服务故障。 */
  @ExceptionHandler(org.springframework.validation.BindException.class)
  public ResponseEntity<ApiResponse<Void>> queryValidation(
      org.springframework.validation.BindException ex) {
    return ResponseEntity.unprocessableEntity()
        .body(ApiResponse.error("VALIDATION_FAILED", "查询条件缺失或格式无效"));
  }

  @ExceptionHandler({
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class
  })
  public ResponseEntity<ApiResponse<Void>> queryFormat(Exception ex) {
    return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "查询参数格式无效"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) {
    // 不直接打印异常 message/stack：第三方异常可能带 URL、code、SQL 参数等敏感数据。
    LOG.error("请求处理失败，异常类型={}", ex.getClass().getSimpleName());
    return ResponseEntity.status(500).body(ApiResponse.error("INTERNAL_ERROR", "服务处理失败，请稍后重试"));
  }
}
