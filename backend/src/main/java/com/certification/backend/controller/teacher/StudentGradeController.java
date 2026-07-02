package com.certification.backend.controller.teacher;

import com.certification.backend.dto.request.GradeWeightRequest;
import com.certification.backend.dto.request.StudentGradeBatchRequest;
import com.certification.backend.dto.response.GradeWeightResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.dto.response.StudentGradeResponse;
import com.certification.backend.service.StudentGradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 学生成绩管理控制器（教师权限）
 * 管理四列固定成绩：平时成绩、实验报告、期中考试、期末考试
 */
@Tag(name = "12-学生成绩管理", description = "教师端学生成绩录入、查询、批量导入导出")
@RestController
@RequestMapping("/teacher/student-grades")
@PreAuthorize("hasAuthority('course:enter-grade')")
public class StudentGradeController {

    private final StudentGradeService studentGradeService;

    public StudentGradeController(StudentGradeService studentGradeService) {
        this.studentGradeService = studentGradeService;
    }

    @Operation(summary = "查询开课下所有学生的四列成绩",
            description = "返回该开课下所有选课学生及其四列成绩（未录入的也返回）")
    @GetMapping("/offering/{offeringId}")
    public ResponseVO<List<StudentGradeResponse>> listByOffering(@PathVariable Long offeringId) {
        String username = getCurrentUsername();
        return ResponseVO.success(studentGradeService.listByOffering(offeringId, username));
    }

    @Operation(summary = "批量保存学生成绩",
            description = "批量保存/更新所有学生的四列成绩，自动计算最终成绩，并同步到旧成绩表")
    @PostMapping("/batch")
    public ResponseVO<List<StudentGradeResponse>> saveBatch(@Valid @RequestBody StudentGradeBatchRequest request) {
        String username = getCurrentUsername();
        return ResponseVO.success("保存成功", studentGradeService.saveBatch(request, username));
    }

    @Operation(summary = "Excel批量导入学生成绩",
            description = "上传 Excel 文件批量导入四列成绩。列顺序：学号、平时成绩、实验报告、期中考试、期末考试")
    @PostMapping("/import/{offeringId}")
    public ResponseVO<String> importGrades(@PathVariable Long offeringId,
                                           @RequestParam("file") MultipartFile file) {
        String username = getCurrentUsername();
        int count = studentGradeService.importGrades(offeringId, file, username);
        return ResponseVO.success(String.format("导入成功，共录入 %d 名学生成绩", count));
    }

    @Operation(summary = "导出学生成绩到 Excel",
            description = "导出七列 Excel：学号、姓名、平时成绩、实验报告、期中考试、期末考试、最终成绩")
    @GetMapping("/export/{offeringId}")
    public void exportGrades(@PathVariable Long offeringId,
                             HttpServletResponse response) throws IOException {
        String username = getCurrentUsername();
        studentGradeService.exportGrades(offeringId, username, response);
    }

    @Operation(summary = "获取成绩权重配置",
            description = "获取开课的四项成绩权重，无配置时返回默认 25%/25%/25%/25%")
    @GetMapping("/weights/{offeringId}")
    public ResponseVO<GradeWeightResponse> getWeightConfig(@PathVariable Long offeringId) {
        String username = getCurrentUsername();
        return ResponseVO.success(studentGradeService.getWeightConfig(offeringId, username));
    }

    @Operation(summary = "保存成绩权重配置",
            description = "保存开课的四项成绩权重")
    @PostMapping("/weights")
    public ResponseVO<GradeWeightResponse> saveWeightConfig(@Valid @RequestBody GradeWeightRequest request) {
        String username = getCurrentUsername();
        return ResponseVO.success("权重配置已保存", studentGradeService.saveWeightConfig(request, username));
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
