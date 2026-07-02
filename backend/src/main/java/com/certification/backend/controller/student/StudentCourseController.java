package com.certification.backend.controller.student;

import com.certification.backend.dto.response.CourseObjectiveResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.dto.response.StudentCourseResponse;
import com.certification.backend.dto.response.StudentGradeResponse;
import com.certification.backend.service.CourseObjectiveService;
import com.certification.backend.service.StudentCourseService;
import com.certification.backend.service.StudentGradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生端课程查看控制器
 */
@Tag(name = "11-学生课程查看", description = "学生查看自己所选课程列表与成绩")
@RestController
@RequestMapping("/student")
@PreAuthorize("hasAuthority('course:detail')")
public class StudentCourseController {

    private final StudentCourseService studentCourseService;
    private final StudentGradeService studentGradeService;
    private final CourseObjectiveService courseObjectiveService;

    public StudentCourseController(StudentCourseService studentCourseService,
                                   StudentGradeService studentGradeService,
                                   CourseObjectiveService courseObjectiveService) {
        this.studentCourseService = studentCourseService;
        this.studentGradeService = studentGradeService;
        this.courseObjectiveService = courseObjectiveService;
    }

    /**
     * 查看当前学生的课程列表
     * GET /api/student/courses
     */
    @Operation(summary = "查看我的课程", description = "根据当前登录 Token 返回该学生选修的所有课程")
    @GetMapping("/courses")
    public ResponseVO<List<StudentCourseResponse>> myCourses() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<StudentCourseResponse> courses = studentCourseService.getStudentCourses(username);
        return ResponseVO.success(courses);
    }

    /**
     * 查看当前学生在指定开课下的四列成绩
     * GET /api/student/grades/{offeringId}
     */
    @Operation(summary = "查看我的成绩", description = "根据当前登录 Token 返回该学生在指定开课下的四列成绩")
    @GetMapping("/grades/{offeringId}")
    public ResponseVO<StudentGradeResponse> myGrade(@PathVariable Long offeringId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseVO.success(studentGradeService.getStudentGrade(offeringId, username));
    }

    /**
     * 查看指定开课下的课程目标
     * GET /api/student/objectives/{offeringId}
     */
    @Operation(summary = "查看课程目标", description = "根据当前登录 Token 和开课ID返回课程目标列表")
    @GetMapping("/objectives/{offeringId}")
    public ResponseVO<List<CourseObjectiveResponse>> myObjectives(@PathVariable Long offeringId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseVO.success(courseObjectiveService.listByOfferingIdForStudent(offeringId, username));
    }
}
