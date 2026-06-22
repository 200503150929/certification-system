package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 指标点新增/编辑请求参数
 */
@Setter
@Getter
public class IndicatorPointRequest {

    private Long id;

    @NotNull(message = "所属毕业要求不能为空")
    private Long requirementId;

    @NotBlank(message = "指标点编码不能为空")
    private String code;

    @NotNull(message = "指标点描述不能为空")
    private String description;

    private BigDecimal weight;

    private BigDecimal passScore;

}