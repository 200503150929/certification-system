package com.certification.backend.repository;

import com.certification.backend.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 考核环节数据访问接口
 */
@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    /**
     * 根据课程目标ID判断是否存在关联的考核环节
     */
    boolean existsByObjectiveId(Long objectiveId);

    /**
     * 根据开课ID查询考核环节列表
     */
    List<Assessment> findByOfferingId(Long offeringId);

    /**
     * 根据开课ID列表批量查询考核环节
     */
    List<Assessment> findByOfferingIdIn(List<Long> offeringIds);
}
