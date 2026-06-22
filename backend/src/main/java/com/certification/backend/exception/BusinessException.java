package com.certification.backend.exception;

import com.certification.backend.enums.ResultCodeEnum;

/**
 * 业务异常，在 Service 层抛出，由 GlobalExceptionHandler 统一捕获处理
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCodeEnum resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCodeEnum resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}