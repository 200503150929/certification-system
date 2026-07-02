package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 学生信息返回参数
 */
@Setter
@Getter
public class StudentInfoResponse {

    /** 学生ID（系统内部ID） */
    private Long studentId;

    /** 学号 */
    private String studentNo;

    /** 学生姓名 */
    private String studentName;

    /** 最终成绩（可能为空） */
    private BigDecimal totalScore;
}