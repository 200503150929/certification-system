package com.certification.backend.repository;

import com.certification.backend.entity.GraduationRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 毕业要求数据访问接口
 */
@Repository
public interface GraduationRequirementRepository extends JpaRepository<GraduationRequirement, Long> {

    /**
     * 根据培养方案ID查询毕业要求列表
     */
    List<GraduationRequirement> findByProgramId(Long programId);

    /**
     * 根据培养方案ID删除所有毕业要求
     */
    void deleteByProgramId(Long programId);
}