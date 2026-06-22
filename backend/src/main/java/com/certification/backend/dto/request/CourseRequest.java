package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 新增/编辑课程请求参数
 */
@Setter
@Getter
public class CourseRequest {

    private Long id;

    @NotBlank(message = "课程代码不能为空")
    private String code;

    @NotBlank(message = "课程名称不能为空")
    private String name;

    @NotNull(message = "学分不能为空")
    private BigDecimal credits;

    private Integer totalHours;

    private Integer theoryHours;

    private Integer labHours;

    private String semester;

    private String courseType;

    private Boolean isRequired;

    @NotNull(message = "培养方案ID不能为空")
    private Long programId;

}