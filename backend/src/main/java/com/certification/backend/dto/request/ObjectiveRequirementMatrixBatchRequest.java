package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 培养目标-毕业要求支撑矩阵批量保存请求
 */
public class ObjectiveRequirementMatrixBatchRequest {

    @NotNull(message = "培养目标ID不能为空")
    private Long objectiveId;

    private List<MatrixItem> items;

    public Long getObjectiveId() {
        return objectiveId;
    }

    public void setObjectiveId(Long objectiveId) {
        this.objectiveId = objectiveId;
    }

    public List<MatrixItem> getItems() {
        return items;
    }

    public void setItems(List<MatrixItem> items) {
        this.items = items;
    }

    public static class MatrixItem {

        private Long requirementId;

        private String supportLevel; // 强/弱

        public Long getRequirementId() {
            return requirementId;
        }

        public void setRequirementId(Long requirementId) {
            this.requirementId = requirementId;
        }

        public String getSupportLevel() {
            return supportLevel;
        }

        public void setSupportLevel(String supportLevel) {
            this.supportLevel = supportLevel;
        }
    }
}