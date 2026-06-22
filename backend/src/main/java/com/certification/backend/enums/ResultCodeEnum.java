package com.certification.backend.enums;

/**
 * 统一响应状态码枚举
 */
public enum ResultCodeEnum {

    SUCCESS(200, "成功"),
    ERROR(500, "系统错误"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    USERNAME_EXISTS(4001, "用户名已存在"),
    LOGIN_FAILED(4002, "用户名或密码错误"),
    USER_DISABLED(4003, "账号已被停用"),
    PASSWORD_RESET_FAILED(4004, "密码重置失败"),
    USER_NOT_FOUND(4005, "用户不存在"),
    OLD_PASSWORD_ERROR(4006, "原密码错误"),
    ROLE_NOT_FOUND(4007, "角色不存在"),
    EXCEL_IMPORT_ERROR(4008, "Excel导入失败"),
    DATA_ERROR(4009, "数据异常");

    private final int code;
    private final String msg;

    ResultCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}