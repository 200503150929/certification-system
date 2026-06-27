package com.certification.backend.repository;

import com.certification.backend.entity.StudentGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学生成绩数据访问接口
 */
@Repository
public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {

    /**
     * 根据开课ID查询所有学生成绩
     */
    List<StudentGrade> findByOfferingId(Long offeringId);

    /**
     * 根据开课ID和学生ID查询单条成绩记录
     */
    Optional<StudentGrade> findByOfferingIdAndStudentId(Long offeringId, Long studentId);

    /**
     * 根据开课ID删除所有成绩记录（开课删除时联动清理）
     */
    void deleteByOfferingId(Long offeringId);
}
