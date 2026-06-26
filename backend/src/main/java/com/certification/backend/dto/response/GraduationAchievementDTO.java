package com.certification.backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 毕业要求达成度结果 DTO
 */
@Data
public class GraduationAchievementDTO {

    /** 毕业要求ID */
    private Long requirementId;

    /** 毕业要求编号 */
    private String code;

    /** 毕业要求描述 */
    private String description;

    /** 整体达成度（取各指标点达成度的最小值） */
    private BigDecimal overallAchievement;

    /** 是否达标 */
    private boolean passed;

    /** 各指标点达成度明细 */
    private List<IndicatorDetail> indicatorDetails;

    @Data
    public static class IndicatorDetail {
        /** 指标点ID */
        private Long indicatorId;

        /** 指标点编号 */
        private String code;

        /** 指标点描述 */
        private String description;

        /** 指标点权重 */
        private BigDecimal weight;

        /** 合格标准 */
        private BigDecimal passScore;

        /** 指标点达成度 */
        private BigDecimal achievement;

        /** 是否达标 */
        private boolean passed;
    }
}