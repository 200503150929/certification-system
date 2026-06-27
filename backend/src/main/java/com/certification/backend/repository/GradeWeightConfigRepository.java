package com.certification.backend.repository;

import com.certification.backend.entity.GradeWeightConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 成绩权重配置数据访问接口
 */
@Repository
public interface GradeWeightConfigRepository extends JpaRepository<GradeWeightConfig, Long> {

    Optional<GradeWeightConfig> findByOfferingId(Long offeringId);
}
