package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 录入/修改成绩请求参数
 */
@Setter
@Getter
public class GradeRequest {

    /** 成绩ID，有值则更新，无值则新增 */
    private Long id;

    @NotNull(message = "开课ID不能为空")
    private Long offeringId;

    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    @NotNull(message = "考核环节ID不能为空")
    private Long assessmentId;

    @NotNull(message = "成绩不能为空")
    private BigDecimal score;
}
