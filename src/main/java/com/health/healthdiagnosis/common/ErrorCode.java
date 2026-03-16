package com.health.healthdiagnosis.common;

/**
 * @author WU,Rowan
 * @date 2026/3/5
 */
/**
 * 统一错误码定义
 * 包含 HTTP 标准状态码和自定义业务错误码
 */
public final class ErrorCode {

    private ErrorCode() {
        // 私有构造，防止实例化
    }

    // ================= HTTP 标准状态码 =================

    /**
     * 成功
     */
    public static final int SUCCESS = 200;

    /**
     * 客户端请求错误 (参数错误、业务逻辑错误)
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 未授权 (Token 无效或过期)
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 禁止访问 (权限不足)
     */
    public static final int FORBIDDEN = 403;

    /**
     * 资源不存在
     */
    public static final int NOT_FOUND = 404;

    /**
     * 服务器内部错误
     */
    public static final int SERVER_ERROR = 500;

    // ================= 业务错误码 (1000+) =================

    /**
     * 用户不存在
     */
    public static final int USER_NOT_FOUND = 1001;

    /**
     * 密码错误
     */
    public static final int WRONG_PASSWORD = 1002;

    /**
     * 用户名已存在
     */
    public static final int USERNAME_EXISTS = 1003;

    /**
     * Token 已过期
     */
    public static final int TOKEN_EXPIRED = 1004;

    /**
     * 问诊会话不存在
     */
    public static final int CONSULTATION_NOT_FOUND = 1005;

    /**
     * 访问被拒绝 (通常指资源归属权校验失败)
     */
    public static final int ACCESS_DENIED = 1006;

    /**
     * 问诊会话已完成，无法进行操作
     */
    public static final int CONSULTATION_ALREADY_COMPLETED = 1007;

    /**
     * 用户已被禁用
     */
    public static final int USER_BANNED = 1008;

    /**
     * 旧密码错误
     */
    public static final int WRONG_OLD_PASSWORD = 1009;

    /**
     * 不允许操作管理员账号
     */
    public static final int CANNOT_OPERATE_ADMIN = 1010;
}
