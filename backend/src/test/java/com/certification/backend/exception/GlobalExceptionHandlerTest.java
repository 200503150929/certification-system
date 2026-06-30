package com.certification.backend.exception;

import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.enums.ResultCodeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessExceptionReturnsBusinessCodeAndMessage() {
        BusinessException exception = new BusinessException(ResultCodeEnum.USER_NOT_FOUND, "user missing");

        ResponseVO<Void> response = handler.handleBusinessException(exception);

        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getCode()).isEqualTo(ResultCodeEnum.USER_NOT_FOUND.getCode());
        assertThat(response.getInfo()).isEqualTo("user missing");
        assertThat(response.getData()).isNull();
    }

    @Test
    void handleAccessDeniedExceptionReturnsForbidden() {
        ResponseVO<Void> response = handler.handleAccessDeniedException(new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getCode()).isEqualTo(ResultCodeEnum.FORBIDDEN.getCode());
        assertThat(response.getInfo()).isEqualTo(ResultCodeEnum.FORBIDDEN.getMsg());
    }

    @Test
    void handleExceptionReturnsGenericError() {
        ResponseVO<Void> response = handler.handleException(new RuntimeException("boom"));

        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getCode()).isEqualTo(ResultCodeEnum.ERROR.getCode());
        assertThat(response.getInfo()).isEqualTo(ResultCodeEnum.ERROR.getMsg());
    }
}
