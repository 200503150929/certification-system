package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 专业/培养方案新增/编辑请求参数
 */
@Setter
@Getter
public class ProgramRequest {

    private Long id;

    @NotBlank(message = "专业名称不能为空")
    private String majorName;

    @NotBlank(message = "版本号不能为空")
    private String version;

    private String status; // draft, published

}