package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 学生课程列表返回参数
 */
@Setter
@Getter
public class StudentCourseResponse {

    private Long offeringId;     // 开课记录ID
    private Long courseId;       // 课程ID
    private String courseCode;   // 课程代码
    private String courseName;   // 课程名称
    private BigDecimal credits;  // 学分
    private String teacherName;  // 授课教师
    private String academicYear; // 学年，如 2025-2026
    private String semester;     // 学期，如 第1学期
    private String courseType;   // 课程类型，如 专业核心
    private Boolean isRequired;  // 是否必修

}
