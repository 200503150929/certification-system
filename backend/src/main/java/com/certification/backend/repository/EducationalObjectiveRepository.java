package com.certification.backend.repository;

import com.certification.backend.entity.EducationalObjective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 培养目标数据访问接口
 */
@Repository
public interface EducationalObjectiveRepository extends JpaRepository<EducationalObjective, Long> {

    /**
     * 根据培养方案ID查询培养目标列表，按排序号排序
     */
    List<EducationalObjective> findByProgramIdOrderBySortOrderAsc(Long programId);

    /**
     * 根据培养方案ID删除所有培养目标
     */
    void deleteByProgramId(Long programId);
}