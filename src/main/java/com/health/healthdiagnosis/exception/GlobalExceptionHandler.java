package com.health.healthdiagnosis.exception;

/*
  @author WU,Rowan
 * @date 2026/3/5
 */

import com.health.healthdiagnosis.common.ErrorCode;
import com.health.healthdiagnosis.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 拦截并处理 Controller 层抛出的各类异常，返回统一格式的 JSON 响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     * 返回对应的业务错误码和消息
     *
     * @param e 业务异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验失败异常 (JSR-303/JSR-380)
     * 对应 @RequestBody 上的 @Valid 或 @Validated 注解失效
     * 返回 400 Bad Request
     *
     * @param e 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        // 获取第一个校验错误的消息
        String defaultMessage = "参数校验失败";
        if (e.getBindingResult().hasErrors()) {
            defaultMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        log.warn("参数校验失败: {}", defaultMessage);
        return Result.error(ErrorCode.BAD_REQUEST, defaultMessage);
    }

    /**
     * 处理绑定异常 (通常针对 @ModelAttribute 或表单提交)
     * 返回 400 Bad Request
     *
     * @param e 绑定异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String defaultMessage = "参数绑定失败";
        if (e.getBindingResult().hasErrors()) {
            defaultMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        log.warn("参数绑定失败: {}", defaultMessage);
        return Result.error(ErrorCode.BAD_REQUEST, defaultMessage);
    }

    /**
     * 处理其他所有未捕获的异常
     * 返回 500 Server Error，并记录完整堆栈信息
     *
     * @param e 未知异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleGeneralException(Exception e) {
        log.error("服务器内部错误:", e);
        return Result.error(ErrorCode.SERVER_ERROR, "服务器内部错误：" + e.getMessage());
    }
}