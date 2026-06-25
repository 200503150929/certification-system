package com.certification.backend.controller.teacher;

import com.certification.backend.dto.request.GradeRequest;
import com.certification.backend.dto.response.GradeResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.service.GradeService;
import com.certification.backend.util.ExcelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 成绩管理控制器（教师权限）
 */
@Tag(name = "08-成绩管理", description = "教师端成绩的录入、查询与修改")
@RestController
@RequestMapping("/teacher/grades")
@PreAuthorize("hasRole('TEACHER')")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @Operation(summary = "录入学生成绩")
    @PostMapping
    public ResponseVO<GradeResponse> add(@Valid @RequestBody GradeRequest request) {
        String username = getCurrentUsername();
        return ResponseVO.success(gradeService.add(request, username));
    }

    @Operation(summary = "查询某门开课下所有学生的成绩列表")
    @GetMapping("/offering/{offeringId}")
    public ResponseVO<List<GradeResponse>> listByOffering(@PathVariable Long offeringId) {
        String username = getCurrentUsername();
        return ResponseVO.success(gradeService.listByOfferingId(offeringId, username));
    }

    @Operation(summary = "修改成绩")
    @PutMapping("/{id}")
    public ResponseVO<GradeResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody GradeRequest request) {
        String username = getCurrentUsername();
        return ResponseVO.success(gradeService.update(id, request, username));
    }

    @Operation(summary = "Excel批量导入学生成绩",
            description = "上传 Excel 文件，按考核环节批量录入成绩。Excel 格式：第1行表头，第1列学号，第2列分数")
    @PostMapping("/import/{assessmentId}")
    public ResponseVO<String> importGrades(@PathVariable Long assessmentId,
                                           @RequestParam("file") MultipartFile file) {
        String username = getCurrentUsername();

        // 解析 Excel
        List<ExcelUtil.GradeImportRow> gradeRows = ExcelUtil.readGradesFromExcel(file);

        if (gradeRows.isEmpty()) {
            return ResponseVO.error(400, "Excel 文件中无有效数据");
        }

        // 调用 Service 批量导入
        int successCount = gradeService.importGrades(assessmentId, gradeRows, username);

        return ResponseVO.success(String.format("导入成功，共录入 %d 条成绩", successCount));
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
