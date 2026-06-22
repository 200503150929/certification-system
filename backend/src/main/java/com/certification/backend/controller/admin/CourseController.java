package com.certification.backend.controller.admin;

import com.certification.backend.dto.request.CourseRequest;
import com.certification.backend.dto.request.CourseRequirementMatrixBatchRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.CourseDetailResponse;
import com.certification.backend.dto.response.CourseResponse;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.service.CourseService;
import com.alibaba.excel.EasyExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 课程体系管理控制器（管理员权限）
 *
 * 涵盖：课程基础信息管理、课程-毕业要求指标点支撑矩阵管理
 */
@Tag(name = "04-课程体系管理", description = "课程基础信息管理、课程-毕业要求指标点支撑矩阵管理")
@RestController
@RequestMapping("/admin/course")
@PreAuthorize("hasRole('ADMIN')")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(summary = "分页查询课程列表", description = "支持按关键词模糊搜索（课程代码/名称），按培养方案过滤")
    @GetMapping("/list")
    public ResponseVO<PageResult<CourseResponse>> listCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long programId,
            @Valid PageQuery pageQuery) {
        PageResult<CourseResponse> result = courseService.listCourses(keyword, programId, pageQuery);
        return ResponseVO.success(result);
    }

    @Operation(summary = "查询课程详情（含支撑矩阵）")
    @GetMapping("/detail/{id}")
    public ResponseVO<CourseDetailResponse> detail(@PathVariable Long id) {
        return ResponseVO.success(courseService.getCourseDetail(id));
    }

    @Operation(summary = "新增课程")
    @PostMapping("/add")
    public ResponseVO<CourseResponse> add(@Valid @RequestBody CourseRequest request) {
        return ResponseVO.success(courseService.addCourse(request));
    }

    @Operation(summary = "编辑课程")
    @PutMapping("/update")
    public ResponseVO<CourseResponse> update(@Valid @RequestBody CourseRequest request) {
        if (request.getId() == null) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "课程ID不能为空");
        }
        return ResponseVO.success(courseService.updateCourse(request));
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/delete/{id}")
    public ResponseVO<Void> delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseVO.success();
    }

    @Operation(summary = "批量保存支撑矩阵", description = "全量替换某课程的毕业要求指标点支撑关系（先删后插）")
    @PostMapping("/matrix/batch-save")
    public ResponseVO<List<CourseDetailResponse.MatrixItemResponse>> batchSaveMatrix(
            @Valid @RequestBody CourseRequirementMatrixBatchRequest request) {
        return ResponseVO.success(courseService.batchSaveMatrix(request));
    }

    @Operation(summary = "导出课程体系及矩阵", description = "导出课程基本信息及支撑矩阵（Excel格式）")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        List<CourseResponse> courses = courseService.listForExport();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("课程体系数据", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), CourseResponse.class)
                .sheet("课程列表")
                .doWrite(courses);
    }
}