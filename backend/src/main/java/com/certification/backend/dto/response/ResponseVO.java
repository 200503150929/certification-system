package com.certification.backend.dto.response;

import com.certification.backend.enums.ResultCodeEnum;

/**
 * 统一响应体
 */
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}