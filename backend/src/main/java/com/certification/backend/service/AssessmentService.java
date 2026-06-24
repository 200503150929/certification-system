package com.certification.backend.service;

import com.certification.backend.dto.request.AssessmentRequest;
import com.certification.backend.dto.response.AssessmentResponse;

import java.util.List;

/**
 * 考核管理业务接口
 */
public interface AssessmentService {

    /**
     * 新增考核方式（按关联的课程目标ID逐个创建记录）
     */
    List<AssessmentResponse> add(AssessmentRequest request, String username);

    /**
     * 查询指定开课记录下的所有考核方式
     */
    List<AssessmentResponse> listByOfferingId(Long offeringId, String username);

    /**
     * 删除指定的考核方式
     */
    void delete(Long id, String username);
}
