package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 学生成绩返回参数（含学生基本信息）
 */
@Setter
@Getter
public class StudentGradeResponse {

    private Long id;
    private Long offeringId;
    private Long studentId;

    /** 学号（username） */
    private String studentNo;

    /** 姓名 */
    private String studentName;

    /** 平时成绩 */
    private BigDecimal dailyScore;

    /** 实验报告 */
    private BigDecimal reportScore;

    /** 期中考试 */
    private BigDecimal midtermScore;

    /** 期末考试 */
    private BigDecimal finalScore;

    /** 最终成绩 */
    private BigDecimal totalScore;

    private String createdAt;
    private String updatedAt;
}
