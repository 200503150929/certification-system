package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
    /** 用户的权限标识符列表，用于前端权限判断 */
    private List<String> permissions;

    public LoginResponse() {
    }

    public LoginResponse(String token, Long userId, String username, String name, String role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.role = role;
    }

    public LoginResponse(String token, Long userId, String username, String name, String role,
                         List<String> permissions) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.role = role;
        this.permissions = permissions;
    }
}
