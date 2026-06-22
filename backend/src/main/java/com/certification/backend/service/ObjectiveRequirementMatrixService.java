package com.certification.backend.service;

import com.certification.backend.dto.request.ObjectiveRequirementMatrixBatchRequest;
import com.certification.backend.dto.request.ObjectiveRequirementMatrixRequest;
import com.certification.backend.dto.response.ObjectiveRequirementMatrixResponse;

import java.util.List;

/**
 * 培养目标-毕业要求支撑矩阵业务接口
 */
public interface ObjectiveRequirementMatrixService {

    /**
     * 根据培养目标ID查询支撑关系列表
     */
    List<ObjectiveRequirementMatrixResponse> listByObjectiveId(Long objectiveId);

    /**
     * 根据毕业要求ID查询支撑关系列表
     */
    List<ObjectiveRequirementMatrixResponse> listByRequirementId(Long requirementId);

    /**
     * 新增支撑关系
     */
    ObjectiveRequirementMatrixResponse add(ObjectiveRequirementMatrixRequest request);

    /**
     * 编辑支撑关系
     */
    ObjectiveRequirementMatrixResponse update(ObjectiveRequirementMatrixRequest request);

    /**
     * 删除支撑关系
     */
    void delete(Long id);

    /**
     * 批量保存支撑关系（先删后插，全量替换）
     */
    List<ObjectiveRequirementMatrixResponse> batchSave(ObjectiveRequirementMatrixBatchRequest request);
}