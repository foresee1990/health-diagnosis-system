package com.health.healthdiagnosis.exception;

/**
 * @author WU,Rowan
 * @date 2026/3/5
 */
import lombok.Getter;

/**
 * 自定义业务异常
 * 用于抛出特定的业务逻辑错误，携带错误码和消息
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造方法
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造方法 (仅传入错误码，消息可为空或后续处理)
     *
     * @param code 错误码
     */
    public BusinessException(int code) {
        super("Business Error: " + code);
        this.code = code;
        this.message = "业务异常";
    }
}
