package com.certification.backend.service;

import com.certification.backend.dto.request.AdminCourseOfferingRequest;
import com.certification.backend.dto.request.CourseOfferingRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.CourseOfferingResponse;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.TeacherSimpleResponse;

import java.util.List;

/**
 * 开课信息管理业务接口
 */
public interface CourseOfferingService {

    PageResult<CourseOfferingResponse> listOfferings(String username, String keyword, PageQuery pageQuery);

    CourseOfferingResponse getOfferingDetail(Long id, String username);

    CourseOfferingResponse addOffering(CourseOfferingRequest request, String username);

    CourseOfferingResponse updateOffering(CourseOfferingRequest request, String username);

    void deleteOffering(Long id, String username);
    // CourseOfferingService.java 新增

    /**
     * 管理员：按课程ID分页查询开课记录
     */
    PageResult<CourseOfferingResponse> listByCourseId(Long courseId, PageQuery pageQuery);

    /**
     * 管理员：新增开课记录（可指定教师）
     */
    CourseOfferingResponse addOfferingByAdmin(AdminCourseOfferingRequest request);

    /**
     * 管理员：更新开课记录
     */
    CourseOfferingResponse updateOfferingByAdmin(AdminCourseOfferingRequest request);

    /**
     * 管理员：删除开课记录
     */
    void deleteOfferingByAdmin(Long id);

    /**
     * 获取所有教师列表
     */
    List<TeacherSimpleResponse> listTeachers();
}
