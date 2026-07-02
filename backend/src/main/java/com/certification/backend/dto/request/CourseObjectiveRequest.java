package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 新增/编辑课程目标请求参数
 */
@Setter
@Getter
public class CourseObjectiveRequest {

    private Long id;

    private String code;

    @NotBlank(message = "目标描述不能为空")
    private String description;

    private BigDecimal weight;
}
