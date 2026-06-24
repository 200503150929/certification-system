package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 课程目标-指标点矩阵项请求参数
 */
@Setter
@Getter
public class MatrixItemRequest {

    @NotNull(message = "课程目标ID不能为空")
    private Long objectiveId;

    @NotNull(message = "指标点ID不能为空")
    private Long indicatorId;

    @NotBlank(message = "支撑强度不能为空")
    private String supportLevel;
}
