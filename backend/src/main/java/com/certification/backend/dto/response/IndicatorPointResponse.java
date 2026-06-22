package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 指标点返回参数
 */
@Setter
@Getter
public class IndicatorPointResponse {

    private Long id;
    private Long requirementId;
    private String code;
    private String description;
    private BigDecimal weight;
    private BigDecimal passScore;
    private String createdAt;

}