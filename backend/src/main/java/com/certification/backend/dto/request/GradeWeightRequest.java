package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * 成绩权重配置请求
 */
@Setter
@Getter
public class GradeWeightRequest {

    @NotNull(message = "开课ID不能为空")
    private Long offeringId;

    /** 平时成绩权重 (0~1) */
    private BigDecimal dailyWeight;

    /** 实验报告权重 (0~1) */
    private BigDecimal reportWeight;

    /** 期中考试权重 (0~1) */
    private BigDecimal midtermWeight;

    /** 期末考试权重 (0~1) */
    private BigDecimal finalWeight;
}
