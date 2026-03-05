package com.health.healthdiagnosis.common;

/**
 * @author WU,Rowan
 * @date 2026/3/5
 */

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 统一响应结果封装类
 * 遵循 doc/api_design.md 规范
 *
 * @param <T> 数据泛型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 状态码 (HTTP 状态码或业务状态码)
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳 (毫秒)
     * 默认值：System.currentTimeMillis()
     */
    private long timestamp;

    /**
     * 构造时自动设置当前时间戳
     */
    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应工厂方法
     *
     * @param data 业务数据
     * @return 成功结果对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS, "成功", data);
    }

    /**
     * 成功响应工厂方法 (无数据)
     *
     * @return 成功结果对象
     */
    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS, "成功", null);
    }

    /**
     * 错误响应工厂方法
     *
     * @param code    错误码
     * @param message 错误消息
     * @return 错误结果对象
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 错误响应工厂方法 (使用 ErrorCode 枚举/常量)
     *
     * @param errorCode 错误码常量
     * @return 错误结果对象
     */
    public static <T> Result<T> error(int errorCode) {
        String msg = "操作失败";
        // 简单映射常见错误消息，实际项目中可结合 ErrorCode 类扩展获取消息的方法
        if (errorCode == ErrorCode.BAD_REQUEST) msg = "请求参数错误";
        else if (errorCode == ErrorCode.UNAUTHORIZED) msg = "未授权";
        else if (errorCode == ErrorCode.FORBIDDEN) msg = "禁止访问";
        else if (errorCode == ErrorCode.NOT_FOUND) msg = "资源不存在";
        else if (errorCode == ErrorCode.SERVER_ERROR) msg = "服务器内部错误";

        return new Result<>(errorCode, msg, null);
    }
}