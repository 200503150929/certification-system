package com.certification.backend.repository;

import com.certification.backend.entity.StudentCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 学生选课数据访问接口
 */
@Repository
public interface StudentCourseRepository extends JpaRepository<StudentCourse, Long>, JpaSpecificationExecutor<StudentCourse> {

    /**
     * 根据开课ID查询学生选课记录
     */
    List<StudentCourse> findByOfferingId(Long offeringId);

    /**
     * 判断是否存在指定开课的学生选课记录
     */
    boolean existsByOfferingId(Long offeringId);

    /**
     * 根据学生ID查询选课记录
     */
    List<StudentCourse> findByStudentId(Long studentId);

    /**
     * 根据开课ID删除所有选课记录
     */
    void deleteByOfferingId(Long offeringId);

    /**
     * 根据学生ID和开课ID查询选课记录（用于校验学生是否选修了该课程）
     */
    java.util.Optional<StudentCourse> findByStudentIdAndOfferingId(Long studentId, Long offeringId);
}
