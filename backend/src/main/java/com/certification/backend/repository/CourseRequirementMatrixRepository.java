package com.certification.backend.repository;

import com.certification.backend.entity.CourseRequirementMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 课程-毕业要求指标点支撑矩阵数据访问接口
 */
@Repository
public interface CourseRequirementMatrixRepository extends JpaRepository<CourseRequirementMatrix, Long> {

    /**
     * 根据课程ID查询支撑矩阵
     */
    List<CourseRequirementMatrix> findByCourseId(Long courseId);

    /**
     * 根据课程ID删除支撑矩阵
     */
    void deleteByCourseId(Long courseId);

    /**
     * 根据课程ID列表批量删除支撑矩阵
     */
    void deleteByCourseIdIn(List<Long> courseIds);

    /**
     * 根据指标点ID查询支撑矩阵
     */
    List<CourseRequirementMatrix> findByIndicatorId(Long indicatorId);
}