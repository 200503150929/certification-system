package com.certification.backend.service;

import com.certification.backend.dto.response.StudentCourseResponse;

import java.util.List;

/**
 * 学生课程服务接口
 */
public interface StudentCourseService {

    /**
     * 根据学生用户名查询其所有选课记录
     *
     * @param username 学生用户名（学号）
     * @return 学生课程列表
     */
    List<StudentCourseResponse> getStudentCourses(String username);

}
