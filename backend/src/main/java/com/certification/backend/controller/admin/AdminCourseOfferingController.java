package com.certification.backend.controller.admin;

import com.certification.backend.dto.request.AdminCourseOfferingRequest;
import com.certification.backend.dto.request.CourseOfferingRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.CourseOfferingResponse;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.dto.response.TeacherSimpleResponse;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.service.CourseOfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 开课信息管理控制器（管理员权限）
 * 用于管理员为课程分配授课教师
 */
@Tag(name = "01-开课信息管理(管理员)", description = "管理员开课记录的增删改查")
@RestController
@RequestMapping("/admin/course/offering")
@PreAuthorize("hasAuthority('course:manage')")
public class AdminCourseOfferingController {

    private final CourseOfferingService courseOfferingService;

    public AdminCourseOfferingController(CourseOfferingService courseOfferingService) {
        this.courseOfferingService = courseOfferingService;
    }

    @Operation(summary = "分页查询课程的开课记录", description = "管理员查看某课程所有开课记录")
    @GetMapping("/list/{courseId}")
    public ResponseVO<PageResult<CourseOfferingResponse>> listByCourse(
            @PathVariable Long courseId,
            @Valid PageQuery pageQuery) {
        PageResult<CourseOfferingResponse> result = courseOfferingService.listByCourseId(courseId, pageQuery);
        return ResponseVO.success(result);
    }

    @Operation(summary = "新增开课记录", description = "管理员为课程分配授课教师")
    @PostMapping("/add")
    public ResponseVO<CourseOfferingResponse> add(@Valid @RequestBody AdminCourseOfferingRequest request) {
        return ResponseVO.success(courseOfferingService.addOfferingByAdmin(request));
    }

    @Operation(summary = "编辑开课记录", description = "管理员修改开课记录")
    @PutMapping("/update")
    public ResponseVO<CourseOfferingResponse> update(@Valid @RequestBody AdminCourseOfferingRequest request) {
        if (request.getId() == null) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "开课记录ID不能为空");
        }
        return ResponseVO.success(courseOfferingService.updateOfferingByAdmin(request));
    }

    @Operation(summary = "删除开课记录")
    @DeleteMapping("/delete/{id}")
    public ResponseVO<Void> delete(@PathVariable Long id) {
        courseOfferingService.deleteOfferingByAdmin(id);
        return ResponseVO.success();
    }

    @Operation(summary = "获取所有教师列表", description = "用于下拉选择授课教师")
    @GetMapping("/teachers")
    public ResponseVO<List<TeacherSimpleResponse>> listTeachers() {
        return ResponseVO.success(courseOfferingService.listTeachers());
    }
}