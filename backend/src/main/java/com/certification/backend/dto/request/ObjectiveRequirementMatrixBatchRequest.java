package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 培养目标-毕业要求支撑矩阵批量保存请求
 */
@Setter
@Getter
public class ObjectiveRequirementMatrixBatchRequest {

    @NotNull(message = "培养目标ID不能为空")
    private Long objectiveId;

    private List<MatrixItem> items;

    @Setter
    @Getter
    public static class MatrixItem {

        private Long requirementId;

        private String supportLevel; // 强/弱

    }
}