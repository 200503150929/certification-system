package com.certification.backend.repository;

import com.certification.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 课程数据访问接口
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    /**
     * 根据培养方案ID查询课程列表
     */
    List<Course> findByProgramId(Long programId);

    /**
     * 检查课程代码是否已存在
     */
    boolean existsByCode(String code);

    /**
     * 检查课程代码是否已存在（排除指定ID）
     */
    boolean existsByCodeAndIdNot(String code, Long id);
}