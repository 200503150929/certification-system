package com.certification.backend.service;

import com.certification.backend.dto.request.CourseObjectiveRequest;
import com.certification.backend.dto.response.CourseObjectiveResponse;

import java.util.List;

/**
 * 课程目标管理业务接口
 */
public interface CourseObjectiveService {

    /**
     * 查询指定开课记录下的所有课程目标
     */
    List<CourseObjectiveResponse> listByOfferingId(Long offeringId, String username);

    /**
     * 学生端查询课程目标（验证学生选修了该课程）
     */
    List<CourseObjectiveResponse> listByOfferingIdForStudent(Long offeringId, String username);

    /**
     * 新增课程目标
     */
    CourseObjectiveResponse add(Long offeringId, CourseObjectiveRequest request, String username);

    /**
     * 编辑课程目标
     */
    CourseObjectiveResponse update(Long id, CourseObjectiveRequest request, String username);

    /**
     * 删除课程目标
     */
    void delete(Long id, String username);
}
