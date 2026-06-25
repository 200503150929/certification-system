package com.certification.backend.service;

import com.certification.backend.dto.module7.StudentCourseDTO;
import com.certification.backend.dto.module7.TeacherStudentDTO;
import com.certification.backend.dto.module7.UserInfoDTO;
import com.certification.backend.dto.module7.UserUpdateDTO;
import java.util.List;

public interface Module7Service {
    // 1. 获取当前登录用户个人信息
    UserInfoDTO getCurrentUserInfo(Long userId);

    // 2. 修改个人手机号、邮箱
    void updateUserInfo(Long userId, UserUpdateDTO dto);

    // 3. 学生：查询自己全部选课列表
    List<StudentCourseDTO> getStudentAllCourse(Long studentId);

    // 4. 学生：单门课程详情
    StudentCourseDTO getCourseDetail(Long studentId, Long offeringId);

    // 5. 教师：根据开课ID查看本班所有学生
    List<TeacherStudentDTO> getTeacherCourseStudent(Long teacherId, Long offeringId);
}