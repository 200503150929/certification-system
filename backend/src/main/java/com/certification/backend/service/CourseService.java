package com.certification.backend.service;

import com.certification.backend.dto.request.CourseRequest;
import com.certification.backend.dto.request.CourseRequirementMatrixBatchRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.CourseDetailResponse;
import com.certification.backend.dto.response.CourseResponse;
import com.certification.backend.dto.response.PageResult;

import java.util.List;

public interface CourseService {

    PageResult<CourseResponse> listCourses(String keyword, Long programId, PageQuery pageQuery);

    CourseDetailResponse getCourseDetail(Long id);

    CourseResponse addCourse(CourseRequest request);

    CourseResponse updateCourse(CourseRequest request);

    void deleteCourse(Long id);

    List<CourseDetailResponse.MatrixItemResponse> batchSaveMatrix(CourseRequirementMatrixBatchRequest request);

    /**
     * 导出课程列表（支持按培养方案过滤）
     *
     * @param programId 培养方案ID，为 null 时导出全部
     * @return 课程列表
     */
    List<CourseResponse> listForExport(Long programId);
}