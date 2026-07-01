package com.certification.backend.repository;

import com.certification.backend.entity.CourseOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    /**
     * 查询所有不重复的学年-学期组合（用于看板学期下拉选项）
     */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT new com.certification.backend.dto.response.SemesterOptionDTO(" +
            "co.academicYear, co.semester) " +
            "FROM CourseOffering co ORDER BY co.academicYear DESC, co.semester DESC")
    List<com.certification.backend.dto.response.SemesterOptionDTO> findDistinctSemesters();

    /**
     * 按学年和学期查询开课记录
     */
    List<CourseOffering> findByAcademicYearAndSemester(String academicYear, String semester);
    // CourseOfferingRepository.java 新增

    /**
     * 分页查询某课程的所有开课记录
     */
    Page<CourseOffering> findByCourseId(Long courseId, PageRequest pageRequest);
}
