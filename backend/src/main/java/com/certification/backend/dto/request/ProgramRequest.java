package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 专业/培养方案新增/编辑请求参数
 */
public class ProgramRequest {

    private Long id;

    @NotBlank(message = "专业名称不能为空")
    private String majorName;

    @NotBlank(message = "版本号不能为空")
    private String version;

    private String status; // draft, published

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}