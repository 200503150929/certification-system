package com.certification.backend.controller.teacher;

import com.certification.backend.dto.request.AssessmentRequest;
import com.certification.backend.dto.response.AssessmentResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考核管理控制器（教师权限）
 */
@Tag(name = "06-考核管理", description = "教师端考核方式的增删查")
@RestController
@RequestMapping("/teacher/assessments")
@PreAuthorize("hasRole('TEACHER')")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @Operation(summary = "新增考核方式")
    @PostMapping
    public ResponseVO<List<AssessmentResponse>> add(@Valid @RequestBody AssessmentRequest request) {
        String username = getCurrentUsername();
        return ResponseVO.success(assessmentService.add(request, username));
    }

    @Operation(summary = "查询某门开课的所有考核方式")
    @GetMapping("/offering/{offeringId}")
    public ResponseVO<List<AssessmentResponse>> listByOffering(@PathVariable Long offeringId) {
        String username = getCurrentUsername();
        return ResponseVO.success(assessmentService.listByOfferingId(offeringId, username));
    }

    @Operation(summary = "删除指定的考核方式")
    @DeleteMapping("/{id}")
    public ResponseVO<Void> delete(@PathVariable Long id) {
        String username = getCurrentUsername();
        assessmentService.delete(id, username);
        return ResponseVO.success();
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
