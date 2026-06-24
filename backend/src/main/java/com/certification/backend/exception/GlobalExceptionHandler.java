package com.certification.backend.exception;

import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.enums.ResultCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器，统一返回 ResponseVO
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseVO<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseVO.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验失败（@Valid 注解）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseVO<Void> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), msg);
    }

    /**
     * Spring Security 权限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseVO<Void> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseVO.error(ResultCodeEnum.FORBIDDEN);
    }

    /**
     * 资源不存在（404）- URL 路径不匹配任何 Controller 映射
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseVO<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("请求路径不存在: {}", e.getResourcePath());
        return ResponseVO.error(ResultCodeEnum.NOT_FOUND.getCode(), "请求路径不存在: " + e.getResourcePath());
    }

    /**
     * 资源不存在（404）- 无 Handler 处理
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseVO<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("请求路径不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return ResponseVO.error(ResultCodeEnum.NOT_FOUND.getCode(), "请求路径不存在");
    }

    /**
     * 其他未捕获异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseVO<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseVO.error(ResultCodeEnum.ERROR);
    }
}