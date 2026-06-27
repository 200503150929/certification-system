package com.certification.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量保存学生成绩请求
 */
@Setter
@Getter
public class StudentGradeBatchRequest {

    @NotNull(message = "开课ID不能为空")
    private Long offeringId;

    @NotNull(message = "成绩列表不能为空")
    @Size(min = 1, message = "至少需要一条成绩记录")
    @Valid
    private List<GradeItem> grades;

    /**
     * 单条成绩项
     */
    @Setter
    @Getter
    public static class GradeItem {
        @NotNull(message = "学生ID不能为空")
        private Long studentId;

        /** 平时成绩 */
        private BigDecimal dailyScore;

        /** 实验报告 */
        private BigDecimal reportScore;

        /** 期中考试 */
        private BigDecimal midtermScore;

        /** 期末考试 */
        private BigDecimal finalScore;
    }
}
