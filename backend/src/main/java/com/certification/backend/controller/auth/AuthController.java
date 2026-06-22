package com.certification.backend.controller.auth;

import com.certification.backend.dto.request.LoginRequest;
import com.certification.backend.dto.response.LoginResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器：登录接口
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     * POST /api/auth/login
     *
     * @param request 登录请求（用户名、密码）
     * @return Token + 用户基本信息
     */
    @PostMapping("/login")
    public ResponseVO<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseVO.success(response);
    }
}