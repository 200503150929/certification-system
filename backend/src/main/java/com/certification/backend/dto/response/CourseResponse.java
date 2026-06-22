package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 课程列表项返回参数
 */
@Setter
@Getter
public class CourseResponse {

    private Long id;
    private String code;
    private String name;
    private BigDecimal credits;
    private Integer totalHours;
    private Integer theoryHours;
    private Integer labHours;
    private String semester;
    private String courseType;
    private Boolean isRequired;
    private Long programId;
    private String programName;
    private String createdAt;

}