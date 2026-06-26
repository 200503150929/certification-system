package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户个人信息返回参数
 */
@Setter
@Getter
public class UserProfileResponse {

    private Long id;
    private String username;  // 工号/学号
    private String name;
    private String role;      // admin, teacher, student
    private String phone;
    private String email;
    private String department;// 院系

    // 教师特有字段
    private String title;     // 职称
    private String office;    // 办公地点

    // 学生特有字段
    private String major;     // 专业
    private String grade;     // 年级
    private String className; // 班级

}
