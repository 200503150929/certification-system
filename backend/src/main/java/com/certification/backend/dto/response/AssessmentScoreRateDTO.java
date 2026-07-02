package com.certification.backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 考核环节得分率 DTO
 */
@Data
public class AssessmentScoreRateDTO {

    /** 考核环节名称（如：平时作业、期末考试） */
    private String name;

    /** 平均得分率（0~1之间） */
    private BigDecimal scoreRate;
}