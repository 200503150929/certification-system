package com.certification.backend.dto.module7;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserInfoDTO {
    private Long id;
    // 学号/工号
    private String username;
    private String name;
    // 角色 student/teacher/admin
    private String role;
    private String phone;
    private String email;
    private String department;
    // 学生独有字段
    private String majorName;
    private String grade;
    private String className;
    // 教师独有字段
    private String title;
    private Integer status;
    private LocalDateTime createdAt;
}