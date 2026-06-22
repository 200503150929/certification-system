package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 登录成功响应
 */
@Setter
@Getter
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private String name;
    private String role;

    public LoginResponse() {
    }

    public LoginResponse(String token, Long userId, String username, String name, String role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.role = role;
    }

}