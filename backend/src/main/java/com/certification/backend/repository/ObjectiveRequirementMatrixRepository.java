package com.certification.backend.repository;

import com.certification.backend.entity.ObjectiveRequirementMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 培养目标-毕业要求支撑矩阵数据访问接口
 */
@Repository
public interface ObjectiveRequirementMatrixRepository extends JpaRepository<ObjectiveRequirementMatrix, Long> {

    /**
     * 根据培养目标ID查询支撑关系
     */
    List<ObjectiveRequirementMatrix> findByObjectiveId(Long objectiveId);

    /**
     * 根据毕业要求ID查询支撑关系
     */
    List<ObjectiveRequirementMatrix> findByRequirementId(Long requirementId);

    /**
     * 根据培养目标ID列表查询支撑关系
     */
    List<ObjectiveRequirementMatrix> findByObjectiveIdIn(List<Long> objectiveIds);

    /**
     * 根据培养目标ID删除支撑关系
     */
    void deleteByObjectiveId(Long objectiveId);

    /**
     * 根据毕业要求ID删除支撑关系
     */
    void deleteByRequirementId(Long requirementId);
}