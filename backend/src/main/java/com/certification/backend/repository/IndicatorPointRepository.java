package com.certification.backend.repository;

import com.certification.backend.entity.IndicatorPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 指标点数据访问接口
 */
@Repository
public interface IndicatorPointRepository extends JpaRepository<IndicatorPoint, Long> {

    /**
     * 根据毕业要求ID查询指标点列表
     */
    List<IndicatorPoint> findByRequirementId(Long requirementId);

    /**
     * 根据毕业要求ID列表查询指标点列表
     */
    List<IndicatorPoint> findByRequirementIdIn(List<Long> requirementIds);

    /**
     * 根据毕业要求ID删除所有指标点
     */
    void deleteByRequirementId(Long requirementId);
}