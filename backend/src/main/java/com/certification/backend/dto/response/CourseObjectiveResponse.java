package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 课程目标返回参数
 */
@Setter
@Getter
public class CourseObjectiveResponse {

    private Long id;
    private Long offeringId;
    private String description;
    private BigDecimal weight;
    private String createdAt;
}
