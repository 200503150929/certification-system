package com.certification.backend.controller.teacher;

import com.certification.backend.dto.request.GradeRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.GradeResponse;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.service.GradeService;
import com.certification.backend.util.ExcelUtil;
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
        List<ExcelUtil.GradeImportRow> gradeRows = ExcelUtil.readGradesFromExcel(file);
        if (gradeRows.isEmpty()) {
            return ResponseVO.error(400, "Excel 文件中无有效数据");
        }
        int successCount = gradeService.importGrades(assessmentId, gradeRows, username);
        return ResponseVO.success(String.format("导入成功，共录入 %d 条成绩", successCount));
    }

    @Operation(summary = "导出成绩列表到 Excel",
            description = "根据开课 ID 查询全部成绩并生成 Excel 文件下载，表头：学号、姓名、考核环节、分数")
    @GetMapping("/export/{offeringId}")
    public void exportGrades(@PathVariable Long offeringId,
                             HttpServletResponse response) throws IOException {
        String username = getCurrentUsername();
        gradeService.exportGrades(offeringId, username, response);
    }

    @Operation(summary = "保存/更新成绩",
            description = "智能判断新增或更新：有 id 则按 ID 更新；无 id 则按 (assessmentId+studentId) 查找，存在则更新，不存在则新增")
    @PostMapping("/save")
    public ResponseVO<GradeResponse> saveOrUpdate(@Valid @RequestBody GradeRequest request) {
        String username = getCurrentUsername();
        return ResponseVO.success("保存成功", gradeService.saveOrUpdate(request, username));
    }

    @Operation(summary = "分页查询成绩列表",
            description = "根据开课 ID 分页查询成绩，支持按学生姓名模糊搜索")
    @GetMapping("/list")
    public ResponseVO<PageResult<GradeResponse>> listGrades(
            @RequestParam Long offeringId,
            @RequestParam(required = false) String studentName,
            @Valid PageQuery pageQuery) {
        String username = getCurrentUsername();
        return ResponseVO.success(gradeService.listGrades(offeringId, studentName, pageQuery, username));
    }

    @Operation(summary = "删除成绩记录")
    @DeleteMapping("/{id}")
    public ResponseVO<Void> deleteGrade(@PathVariable Long id) {
        String username = getCurrentUsername();
        gradeService.deleteGrade(id, username);
        return ResponseVO.success();
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
