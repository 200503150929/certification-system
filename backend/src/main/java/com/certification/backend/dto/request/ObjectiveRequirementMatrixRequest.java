package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 培养目标-毕业要求支撑矩阵请求参数
 */
public class ObjectiveRequirementMatrixRequest {

    private Long id;

    @NotNull(message = "培养目标ID不能为空")
    private Long objectiveId;

    @NotNull(message = "毕业要求ID不能为空")
    private Long requirementId;

    @NotBlank(message = "支撑强度不能为空")
    private String supportLevel; // 强/弱

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getObjectiveId() {
        return objectiveId;
    }

    public void setObjectiveId(Long objectiveId) {
        this.objectiveId = objectiveId;
    }

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