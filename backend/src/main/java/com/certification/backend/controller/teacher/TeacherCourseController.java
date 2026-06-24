package com.certification.backend.controller.teacher;

import com.certification.backend.dto.request.CourseRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.CourseDetailResponse;
import com.certification.backend.dto.response.CourseResponse;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 课程基本信息管理控制器（教师权限）
 *
 * 复用已有的 CourseService，提供教师端的课程增删改查
 */
@Tag(name = "09-教师端课程基本信息管理", description = "教师端课程基本信息的增删改查")
@RestController
@RequestMapping("/teacher/courses")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherCourseController {

    private final CourseService courseService;

    public TeacherCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(summary = "分页查询课程列表", description = "支持按关键词模糊搜索，按培养方案过滤")
    @GetMapping("/page")
    public ResponseVO<PageResult<CourseResponse>> listCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long programId,
            @Valid PageQuery pageQuery) {
        PageResult<CourseResponse> result = courseService.listCourses(keyword, programId, pageQuery);
        return ResponseVO.success(result);
    }

    @Operation(summary = "查询单门课程详情")
    @GetMapping("/{id}")
    public ResponseVO<CourseDetailResponse> detail(@PathVariable Long id) {
        return ResponseVO.success(courseService.getCourseDetail(id));
    }

    @Operation(summary = "新增一门课程")
    @PostMapping
    public ResponseVO<CourseResponse> add(@Valid @RequestBody CourseRequest request) {
        return ResponseVO.success(courseService.addCourse(request));
    }

    @Operation(summary = "修改课程信息")
    @PutMapping("/{id}")
    public ResponseVO<CourseResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody CourseRequest request) {
        request.setId(id);
        return ResponseVO.success(courseService.updateCourse(request));
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{id}")
    public ResponseVO<Void> delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseVO.success();
    }
}
