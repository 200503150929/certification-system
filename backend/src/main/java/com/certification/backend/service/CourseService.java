package com.certification.backend.service;

import com.certification.backend.dto.request.CourseRequest;
import com.certification.backend.dto.request.CourseRequirementMatrixBatchRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.CourseDetailResponse;
import com.certification.backend.dto.response.CourseResponse;
import com.certification.backend.dto.response.PageResult;

import java.util.List;

/**
 * 课程体系管理业务接口
 */
public interface CourseService {

    /**
     * 分页查询课程列表
     */
    PageResult<CourseResponse> listCourses(String keyword, Long programId, PageQuery pageQuery);

    /**
     * 查询课程详情（含支撑矩阵）
     */
    CourseDetailResponse getCourseDetail(Long id);

    /**
     * 新增课程
     */
    CourseResponse addCourse(CourseRequest request);

    /**
     * 编辑课程
     */
    CourseResponse updateCourse(CourseRequest request);

    /**
     * 删除课程
     */
    void deleteCourse(Long id);

    /**
     * 批量保存支撑矩阵（全量替换）
     */
    List<CourseDetailResponse.MatrixItemResponse> batchSaveMatrix(CourseRequirementMatrixBatchRequest request);

    /**
     * 导出课程体系及矩阵
     */
    List<CourseResponse> listForExport();
}