package com.certification.backend.repository;

import com.certification.backend.entity.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 开课信息数据访问接口
 */
@Repository
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long>, JpaSpecificationExecutor<CourseOffering> {

    List<CourseOffering> findByTeacherId(Long teacherId);

    List<CourseOffering> findByCourseId(Long courseId);

    boolean existsByCourseIdAndTeacherIdAndAcademicYearAndSemester(Long courseId, Long teacherId, String academicYear, String semester);

    boolean existsByCourseIdAndTeacherIdAndAcademicYearAndSemesterAndIdNot(Long courseId, Long teacherId, String academicYear, String semester, Long id);

    void deleteByCourseId(Long courseId);
}
