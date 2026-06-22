package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 毕业要求新增/编辑请求参数
 */
@Setter
@Getter
public class GraduationRequirementRequest {

    private Long id;

    @NotNull(message = "所属培养方案不能为空")
    private Long programId;

    @NotBlank(message = "毕业要求编码不能为空")
    private String code;

    @NotNull(message = "毕业要求描述不能为空")
    private String description;

}