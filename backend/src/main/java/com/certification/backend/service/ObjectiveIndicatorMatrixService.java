package com.certification.backend.service;

import com.certification.backend.dto.request.MatrixItemRequest;
import com.certification.backend.dto.response.MatrixItemResponse;

import java.util.List;

/**
 * 课程目标-指标点支撑矩阵业务接口
 */
public interface ObjectiveIndicatorMatrixService {

    /**
     * 查询指定开课的课程目标与指标点支撑矩阵
     */
    List<MatrixItemResponse> getMatrix(Long offeringId, String username);

    /**
     * 批量保存/更新支撑矩阵（全量替换模式）
     */
    List<MatrixItemResponse> batchSaveMatrix(Long offeringId, List<MatrixItemRequest> items, String username);
}
