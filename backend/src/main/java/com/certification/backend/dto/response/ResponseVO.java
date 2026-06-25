package com.certification.backend.dto.response;

import com.certification.backend.enums.ResultCodeEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * 统一响应体
 */
@Setter
@Getter
public class ResponseVO<T> {

    private String status;
    private int code;
    private String info;
    private T data;

    public ResponseVO() {
    }

    public ResponseVO(String status, int code, String info, T data) {
        this.status = status;
        this.code = code;
        this.info = info;
        this.data = data;
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> ResponseVO<T> success(T data) {
        return new ResponseVO<>("success", ResultCodeEnum.SUCCESS.getCode(),
                ResultCodeEnum.SUCCESS.getMsg(), data);
    }

    /**
     * 成功响应（带自定义消息和数据）
     */
    public static <T> ResponseVO<T> success(String message, T data) {
        return new ResponseVO<>("success", ResultCodeEnum.SUCCESS.getCode(), message, data);
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> ResponseVO<T> success() {
        return success(null);
    }

    /**
     * 业务错误响应
     */
    public static <T> ResponseVO<T> error(ResultCodeEnum resultCode) {
        return new ResponseVO<>("error", resultCode.getCode(), resultCode.getMsg(), null);
    }

    /**
     * 业务错误响应（自定义提示信息）
     */
    public static <T> ResponseVO<T> error(ResultCodeEnum resultCode, String message) {
        return new ResponseVO<>("error", resultCode.getCode(), message, null);
    }

    /**
     * 自定义错误响应
     */
    public static <T> ResponseVO<T> error(int code, String message) {
        return new ResponseVO<>("error", code, message, null);
    }

    // ---------- Getters & Setters ----------

}