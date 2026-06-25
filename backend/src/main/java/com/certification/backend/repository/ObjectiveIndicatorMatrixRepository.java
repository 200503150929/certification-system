package com.certification.backend.repository;

import com.certification.backend.entity.ObjectiveIndicatorMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 课程目标-指标点支撑矩阵数据访问接口
 */
@Repository
public interface ObjectiveIndicatorMatrixRepository extends JpaRepository<ObjectiveIndicatorMatrix, Long> {

    /**
     * 根据课程目标ID查询支撑关系
     */
    List<ObjectiveIndicatorMatrix> findByObjectiveId(Long objectiveId);

    /**
     * 根据指标点ID查询支撑关系
     */
    List<ObjectiveIndicatorMatrix> findByIndicatorId(Long indicatorId);

    /**
     * 根据课程目标ID列表查询支撑关系
     */
    List<ObjectiveIndicatorMatrix> findByObjectiveIdIn(List<Long> objectiveIds);

    /**
     * 根据课程目标ID删除支撑关系
     */
    void deleteByObjectiveId(Long objectiveId);

    /**
     * 根据课程目标ID列表批量删除支撑关系
     */
    void deleteByObjectiveIdIn(List<Long> objectiveIds);
}