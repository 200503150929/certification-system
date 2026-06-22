package com.certification.backend.dto.response;

/**
 * 培养目标-毕业要求支撑矩阵返回参数
 */
public class ObjectiveRequirementMatrixResponse {

    private Long id;
    private Long objectiveId;
    private Long requirementId;
    private String supportLevel;

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