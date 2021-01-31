/** create by Guo Limin on 2021/1/31. */
package com.github.x19990416.mxpaas.admin.common.exception.handler;

import com.github.x19990416.mxpaas.admin.common.exception.BadRequestException;
import com.github.x19990416.mxpaas.admin.common.exception.EntityExistException;
import com.github.x19990416.mxpaas.admin.common.exception.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** 处理所有不可知的异常 */
  @ExceptionHandler(Throwable.class)
  public ResponseEntity<ApiError> handleException(Throwable e) {
      e.printStackTrace();
    // 打印堆栈信息
    log.error(getStackTrace(e));
    return buildResponseEntity(ApiError.error(e.getMessage()));
  }

  /** BadCredentialsException */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> badCredentialsException(BadCredentialsException e) {
    // 打印堆栈信息
      e.printStackTrace();
    String message = "坏的凭证".equals(e.getMessage()) ? "用户名或密码不正确" : e.getMessage();
    log.error(message);
    return buildResponseEntity(ApiError.error(message));
  }

  /** 处理自定义异常 */
  @ExceptionHandler(value = BadRequestException.class)
  public ResponseEntity<ApiError> badRequestException(BadRequestException e) {
    // 打印堆栈信息
      e.printStackTrace();
    log.error(getStackTrace(e));
    return buildResponseEntity(ApiError.error(e.getStatus(), e.getMessage()));
  }

  /** 处理 EntityExist */
  @ExceptionHandler(value = EntityExistException.class)
  public ResponseEntity<ApiError> entityExistException(EntityExistException e) {
    // 打印堆栈信息
      e.printStackTrace();
    log.error(getStackTrace(e));
    return buildResponseEntity(ApiError.error(e.getMessage()));
  }

  /** 处理 EntityNotFound */
  @ExceptionHandler(value = EntityNotFoundException.class)
  public ResponseEntity<ApiError> entityNotFoundException(EntityNotFoundException e) {
    // 打印堆栈信息
      e.printStackTrace();
    log.error(getStackTrace(e));
    return buildResponseEntity(ApiError.error(NOT_FOUND.value(), e.getMessage()));
  }

  /** 处理所有接口数据验证异常 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    // 打印堆栈信息
      e.printStackTrace();
    log.error(getStackTrace(e));
    String[] str =
        Objects.requireNonNull(e.getBindingResult().getAllErrors().get(0).getCodes())[1].split(
            "\\.");
    String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
    String msg = "不能为空";
    if (msg.equals(message)) {
      message = str[1] + ":" + message;
    }
    return buildResponseEntity(ApiError.error(message));
  }

  /** 统一返回 */
  private ResponseEntity<ApiError> buildResponseEntity(ApiError apiError) {
    return new ResponseEntity<>(apiError, HttpStatus.valueOf(apiError.getStatus()));
  }

  private String getStackTrace(Throwable throwable) {
    StringWriter sw = new StringWriter();
    try (PrintWriter pw = new PrintWriter(sw)) {
      throwable.printStackTrace(pw);
      return sw.toString();
    }
  }
}
