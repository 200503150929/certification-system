package com.certification.backend.service;

import com.certification.backend.dto.request.CourseOfferingRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.CourseOfferingResponse;
import com.certification.backend.dto.response.PageResult;

/**
 * 开课信息管理业务接口
 */
public interface CourseOfferingService {

    PageResult<CourseOfferingResponse> listOfferings(String username, String keyword, PageQuery pageQuery);

    CourseOfferingResponse getOfferingDetail(Long id, String username);

    CourseOfferingResponse addOffering(CourseOfferingRequest request, String username);

    CourseOfferingResponse updateOffering(CourseOfferingRequest request, String username);

    void deleteOffering(Long id, String username);
}
