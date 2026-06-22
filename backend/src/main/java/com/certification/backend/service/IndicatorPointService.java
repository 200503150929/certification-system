package com.certification.backend.service;

import com.certification.backend.dto.request.IndicatorPointRequest;
import com.certification.backend.dto.response.IndicatorPointResponse;

import java.util.List;

/**
 * 指标点业务接口
 */
public interface IndicatorPointService {

    /**
     * 根据毕业要求ID查询指标点列表
     */
    List<IndicatorPointResponse> listByRequirementId(Long requirementId);

    /**
     * 查询指标点详情
     */
    IndicatorPointResponse getById(Long id);

    /**
     * 新增指标点
     */
    IndicatorPointResponse add(IndicatorPointRequest request);

    /**
     * 编辑指标点
     */
    IndicatorPointResponse update(IndicatorPointRequest request);

    /**
     * 删除指标点
     */
    void delete(Long id);
}