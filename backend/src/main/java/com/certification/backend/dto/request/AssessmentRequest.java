package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增考核方式请求参数
 */
@Setter
@Getter
public class AssessmentRequest {

    @NotNull(message = "开课ID不能为空")
    private Long offeringId;

    @NotBlank(message = "考核名称不能为空")
    private String assessmentName;

    @NotNull(message = "权重不能为空")
    private BigDecimal weight;

    @NotEmpty(message = "关联课程目标ID列表不能为空")
    private List<Long> objectiveIds;
}
