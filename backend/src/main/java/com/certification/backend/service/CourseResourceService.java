package com.certification.backend.service;

import com.certification.backend.dto.request.CourseResourceRequest;
import com.certification.backend.dto.response.CourseResourceResponse;

import java.util.List;

/**
 * 课程资源管理业务接口
 */
public interface CourseResourceService {

    /**
     * 新增课程资源
     */
    CourseResourceResponse add(CourseResourceRequest request, String username);

    /**
     * 查询指定开课记录下的所有资源
     */
    List<CourseResourceResponse> listByOfferingId(Long offeringId, String username);

    /**
     * 删除指定资源
     */
    void delete(Long id, String username);
}
