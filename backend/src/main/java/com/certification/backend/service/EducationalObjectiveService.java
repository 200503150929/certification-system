package com.certification.backend.service;

import com.certification.backend.dto.request.EducationalObjectiveRequest;
import com.certification.backend.dto.response.EducationalObjectiveResponse;

import java.util.List;

/**
 * 培养目标业务接口
 */
public interface EducationalObjectiveService {

    /**
     * 根据培养方案ID查询培养目标列表
     */
    List<EducationalObjectiveResponse> listByProgramId(Long programId);

    /**
     * 查询培养目标详情
     */
    EducationalObjectiveResponse getById(Long id);

    /**
     * 新增培养目标
     */
    EducationalObjectiveResponse add(EducationalObjectiveRequest request);

    /**
     * 编辑培养目标
     */
    EducationalObjectiveResponse update(EducationalObjectiveRequest request);

    /**
     * 删除培养目标
     */
    void delete(Long id);
}