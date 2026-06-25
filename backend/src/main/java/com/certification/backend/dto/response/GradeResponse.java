package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 成绩返回参数
 */
@Setter
@Getter
public class GradeResponse {

    private Long id;
    private Long assessmentId;
    private String assessmentName;
    private Long studentId;
    private String studentName;
    private BigDecimal score;
    private String createdAt;
}
