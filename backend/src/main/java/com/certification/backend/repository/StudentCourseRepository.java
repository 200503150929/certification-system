package com.certification.backend.repository;

import com.certification.backend.entity.StudentCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学生选课数据访问接口
 */
@Repository
public interface StudentCourseRepository extends JpaRepository<StudentCourse, Long> {

    /**
     * 根据开课ID查询所有选课记录
     */
    List<StudentCourse> findByOfferingId(Long offeringId);

    /**
     * 根据学生ID查询所有选课记录
     */
    List<StudentCourse> findByStudentId(Long studentId);

    /**
     * 根据开课ID和学生ID查询单条选课记录
     */
    Optional<StudentCourse> findByOfferingIdAndStudentId(Long offeringId, Long studentId);

    /**
     * 检查某个学生是否选修了某门开课
     */
    boolean existsByOfferingIdAndStudentId(Long offeringId, Long studentId);

    /**
     * 根据开课ID检查是否存在选课记录
     */
    boolean existsByOfferingId(Long offeringId);

    /**
     * 根据开课ID删除所有选课记录（开课删除时联动清理）
     */
    void deleteByOfferingId(Long offeringId);
}