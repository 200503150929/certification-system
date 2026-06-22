package com.certification.backend.service;

import com.certification.backend.dto.request.LoginRequest;
import com.certification.backend.dto.response.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录，校验密码并生成 JWT Token
     *
     * @param request 登录请求（用户名、密码）
     * @return 登录响应（Token + 用户基本信息）
     */
    LoginResponse login(LoginRequest request);
}