package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户列表返回参数
 */
@Setter
@Getter
public class UserResponse {

    private Long id;
    private String username;
    private String name;
    private String role;
    private String phone;
    private String email;
    private String department;
    private Integer status;
    private String createdAt;

}