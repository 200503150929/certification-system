package com.certification.backend.dto.response;

import lombok.Data;

/**
 * 成绩分布 DTO
 */
@Data
public class GradeDistributionDTO {

    /** 分数区间（如 90-100） */
    private String range;

    /** 分数区间标签（如 优秀） */
    private String label;

    /** 该区间的课程数量 */
    private int count;
}