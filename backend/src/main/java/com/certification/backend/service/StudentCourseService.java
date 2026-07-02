package com.certification.backend.service;

import com.certification.backend.dto.response.ImportResultResponse;
import com.certification.backend.dto.response.StudentCourseResponse;
import com.certification.backend.dto.response.StudentInfoResponse;

import java.util.List;

/**
 * 学生课程服务接口
 */
public interface StudentCourseService {

    /**
     * 根据学生用户名查询其所有选课记录
     */
    List<StudentCourseResponse> getStudentCourses(String username);

    /**
     * 获取开课记录的学生名单
     */
    List<StudentInfoResponse> getStudentsByOffering(Long offeringId, String teacherUsername);

    /**
     * 批量导入学生
     */
    ImportResultResponse importStudents(Long offeringId, List<String> studentNos, String teacherUsername);

    /**
     * 移除单个学生
     */
    void removeStudent(Long offeringId, Long studentId, String teacherUsername);

    /**
     * 批量移除学生
     */
    ImportResultResponse batchRemoveStudents(Long offeringId, List<Long> studentIds, String teacherUsername);
}