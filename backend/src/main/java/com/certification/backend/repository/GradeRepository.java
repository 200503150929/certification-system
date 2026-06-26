package com.certification.backend.repository;

import com.certification.backend.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 成绩数据访问接口
 */
@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    /**
     * 根据考核环节ID查询成绩列表
     */
    List<Grade> findByAssessmentId(Long assessmentId);

    /**
     * 根据考核环节ID列表查询成绩列表
     */
    List<Grade> findByAssessmentIdIn(List<Long> assessmentIds);

    /**
     * 根据学生ID查询成绩列表
     */
    List<Grade> findByStudentId(Long studentId);

    /**
     * 根据考核环节ID和学生ID查找成绩（用于唯一性校验）
     */
    Optional<Grade> findByAssessmentIdAndStudentId(Long assessmentId, Long studentId);
}
