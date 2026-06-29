package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserProfileResponse {

    private Long id;
    private String username;
    private String name;
    private String role;
    private String phone;
    private String email;
    private String college;  // 学院
    private String major;    // 专业

    // 学生特有字段
    private String grade;
    private String className;
}