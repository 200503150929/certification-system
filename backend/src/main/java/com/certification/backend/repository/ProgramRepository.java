package com.certification.backend.repository;

import com.certification.backend.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 专业/培养方案数据访问接口
 */
@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {

    /**
     * 根据专业名称查询
     */
    Optional<Program> findByMajorName(String majorName);

    /**
     * 查询指定状态的专业列表
     */
    java.util.List<Program> findByStatus(String status);
}