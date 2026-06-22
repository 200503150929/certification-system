package com.certification.backend.service;

import com.certification.backend.dto.request.GraduationRequirementRequest;
import com.certification.backend.dto.response.GraduationRequirementResponse;
import com.certification.backend.dto.response.GraduationRequirementDetailResponse;

import java.util.List;

/**
 * 毕业要求业务接口
 */
public interface GraduationRequirementService {

    /**
     * 根据培养方案ID查询毕业要求列表
     */
    List<GraduationRequirementResponse> listByProgramId(Long programId);

    /**
     * 根据培养方案ID查询毕业要求详情列表（含指标点）
     */
    List<GraduationRequirementDetailResponse> listDetailByProgramId(Long programId);

    /**
     * 查询毕业要求详情
     */
    GraduationRequirementResponse getById(Long id);

    /**
     * 新增毕业要求
     */
    GraduationRequirementResponse add(GraduationRequirementRequest request);

    /**
     * 编辑毕业要求
     */
    GraduationRequirementResponse update(GraduationRequirementRequest request);

    /**
     * 删除毕业要求
     */
    void delete(Long id);
}