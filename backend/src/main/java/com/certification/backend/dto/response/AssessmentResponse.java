package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 考核方式返回参数
 */
@Setter
@Getter
public class AssessmentResponse {

    private Long id;
    private Long offeringId;
    private String name;
    private BigDecimal weight;
    private Long objectiveId;
    private String createdAt;
}
