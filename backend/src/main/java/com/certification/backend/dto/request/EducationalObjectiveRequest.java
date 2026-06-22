package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 培养目标新增/编辑请求参数
 */
public class EducationalObjectiveRequest {

    private Long id;

    @NotNull(message = "所属培养方案不能为空")
    private Long programId;

    @NotNull(message = "培养目标描述不能为空")
    private String description;

    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProgramId() {
        return programId;
    }

    public void setProgramId(Long programId) {
        this.programId = programId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}