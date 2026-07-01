package com.certification.backend.controller.teacher;

import com.certification.backend.dto.response.ImportResultResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.dto.response.StudentInfoResponse;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.service.StudentCourseService;
import com.certification.backend.util.ExcelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 教师端 - 学生课程管理控制器
 * 用于教师管理自己所授课程的学生名单
 */
@Tag(name = "05-教师学生管理", description = "教师管理授课课程的学生名单")
@RestController
@RequestMapping("/teacher/student-course")
@PreAuthorize("hasAuthority('course:teach')")
public class TeacherStudentController {

    private final StudentCourseService studentCourseService;

    public TeacherStudentController(StudentCourseService studentCourseService) {
        this.studentCourseService = studentCourseService;
    }

    /**
     * 获取开课记录的学生名单
     * GET /api/teacher/student-course/list/{offeringId}
     */
    @Operation(summary = "获取学生名单", description = "获取指定开课记录的所有学生名单及成绩")
    @GetMapping("/list/{offeringId}")
    public ResponseVO<List<StudentInfoResponse>> listStudents(@PathVariable Long offeringId) {
        String username = getCurrentUsername();
        List<StudentInfoResponse> list = studentCourseService.getStudentsByOffering(offeringId, username);
        return ResponseVO.success(list);
    }

    /**
     * 批量导入学生（JSON 方式）
     * POST /api/teacher/student-course/import/{offeringId}
     */
    @Operation(summary = "批量导入学生", description = "通过 JSON 数组批量导入学生学号")
    @PostMapping("/import/{offeringId}")
    public ResponseVO<ImportResultResponse> importStudents(
            @PathVariable Long offeringId,
            @RequestBody List<String> studentNos) {
        if (studentNos == null || studentNos.isEmpty()) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "学号列表不能为空");
        }
        String username = getCurrentUsername();
        ImportResultResponse result = studentCourseService.importStudents(offeringId, studentNos, username);
        return ResponseVO.success(result);
    }

    /**
     * 批量导入学生（Excel 方式）
     * POST /api/teacher/student-course/import-excel/{offeringId}
     */
    @Operation(summary = "批量导入学生（Excel）", description = "上传 Excel 文件批量导入学生")
    @PostMapping(value = "/import-excel/{offeringId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseVO<ImportResultResponse> importStudentsFromExcel(
            @PathVariable Long offeringId,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "上传文件不能为空");
        }

        String username = getCurrentUsername();

        // 使用 ExcelUtil 读取学号列表
        List<String> studentNos = ExcelUtil.readStudentNosFromExcel(file);

        if (studentNos.isEmpty()) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "未从文件中读取到有效学号");
        }

        ImportResultResponse result = studentCourseService.importStudents(offeringId, studentNos, username);
        return ResponseVO.success(result);
    }

    /**
     * 从开课记录中移除单个学生
     * DELETE /api/teacher/student-course/remove/{offeringId}/{studentId}
     */
    @Operation(summary = "移除学生", description = "将指定学生从开课记录中移除")
    @DeleteMapping("/remove/{offeringId}/{studentId}")
    public ResponseVO<Void> removeStudent(
            @PathVariable Long offeringId,
            @PathVariable Long studentId) {
        String username = getCurrentUsername();
        studentCourseService.removeStudent(offeringId, studentId, username);
        return ResponseVO.success();
    }

    /**
     * 批量移除学生
     * DELETE /api/teacher/student-course/batch-remove/{offeringId}
     */
    @Operation(summary = "批量移除学生", description = "批量将学生从开课记录中移除")
    @DeleteMapping("/batch-remove/{offeringId}")
    public ResponseVO<ImportResultResponse> batchRemoveStudents(
            @PathVariable Long offeringId,
            @RequestBody List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "学生ID列表不能为空");
        }
        String username = getCurrentUsername();
        ImportResultResponse result = studentCourseService.batchRemoveStudents(offeringId, studentIds, username);
        return ResponseVO.success(result);
    }

    /**
     * 获取当前登录用户名
     */
    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}