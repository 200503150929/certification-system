package com.certification.backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 指标点达成度结果 DTO
 */
@Data
public class IndicatorAchievementDTO {

    /** 指标点ID */
    private Long indicatorId;

    /** 指标点编号（如 1.1） */
    private String code;

    /** 指标点描述 */
    private String description;

    /** 指标点权重 */
    private BigDecimal weight;

    /** 合格标准 */
    private BigDecimal passScore;

    /** 指标点达成度（0~1之间） */
    private BigDecimal achievement;

    /** 是否达标 */
    private boolean passed;

    /** 支撑该指标点的课程目标明细 */
    private List<SupportingObjective> supportingObjectives;

    @Data
    public static class SupportingObjective {
        /** 课程目标ID */
        private Long objectiveId;

        /** 课程目标描述 */
        private String description;

        /** 支撑强度 (H/M/L) */
        private String supportLevel;

        /** 课程目标达成度 */
        private BigDecimal achievement;
    }
}
