package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 毕业要求新增/编辑请求参数
 */
public class GraduationRequirementRequest {

    private Long id;

    @NotNull(message = "所属培养方案不能为空")
    private Long programId;

    @NotBlank(message = "毕业要求编码不能为空")
    private String code;

    @NotNull(message = "毕业要求描述不能为空")
    private String description;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}