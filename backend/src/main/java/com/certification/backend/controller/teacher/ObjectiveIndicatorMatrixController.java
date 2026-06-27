package com.certification.backend.controller.teacher;

import com.certification.backend.dto.request.MatrixItemRequest;
import com.certification.backend.dto.response.IndicatorInfoResponse;
import com.certification.backend.dto.response.MatrixItemResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.service.ObjectiveIndicatorMatrixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程目标-指标点支撑矩阵控制器（教师权限）
 */
@Tag(name = "10-课程目标-指标点支撑矩阵", description = "教师端课程目标与指标点支撑矩阵的查询与批量保存")
@RestController
@RequestMapping("/teacher/objectives")
@PreAuthorize("hasRole('TEACHER')")
public class ObjectiveIndicatorMatrixController {

    private final ObjectiveIndicatorMatrixService matrixService;

    public ObjectiveIndicatorMatrixController(ObjectiveIndicatorMatrixService matrixService) {
        this.matrixService = matrixService;
    }

    @Operation(summary = "查询指定开课的课程目标与指标点支撑矩阵")
    @GetMapping("/matrix/{offeringId}")
    public ResponseVO<List<MatrixItemResponse>> getMatrix(@PathVariable Long offeringId) {
        String username = getCurrentUsername();
        return ResponseVO.success(matrixService.getMatrix(offeringId, username));
    }

    @Operation(summary = "批量保存/更新支撑矩阵（全量替换模式）")
    @PostMapping("/matrix/{offeringId}")
    public ResponseVO<List<MatrixItemResponse>> batchSaveMatrix(
            @PathVariable Long offeringId,
            @Valid @RequestBody List<MatrixItemRequest> items) {
        String username = getCurrentUsername();
        return ResponseVO.success(matrixService.batchSaveMatrix(offeringId, items, username));
    }

    @Operation(summary = "获取开课所属专业的所有指标点", description = "供矩阵配置时生成列头")
    @GetMapping("/indicators/{offeringId}")
    public ResponseVO<List<IndicatorInfoResponse>> getIndicators(@PathVariable Long offeringId) {
        String username = getCurrentUsername();
        return ResponseVO.success(matrixService.getIndicatorsByOffering(offeringId, username));
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
