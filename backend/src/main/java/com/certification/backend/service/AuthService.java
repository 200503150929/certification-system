package com.certification.backend.service;

import com.certification.backend.dto.request.LoginRequest;
import com.certification.backend.dto.response.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 修改密码
     *
     * @param username   当前用户名（从 Token 解析）
     * @param oldPassword 原密码
     * @param newPassword 新密码
     */
    void changePassword(String username, String oldPassword, String newPassword);
}