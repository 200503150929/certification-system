package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * 成绩权重配置返回参数
 */
@Setter
@Getter
public class GradeWeightResponse {

    private Long id;
    private Long offeringId;

    /** 平时成绩权重 */
    private BigDecimal dailyWeight;

    /** 实验报告权重 */
    private BigDecimal reportWeight;

    /** 期中考试权重 */
    private BigDecimal midtermWeight;

    /** 期末考试权重 */
    private BigDecimal finalWeight;

    private String createdAt;
    private String updatedAt;
}
