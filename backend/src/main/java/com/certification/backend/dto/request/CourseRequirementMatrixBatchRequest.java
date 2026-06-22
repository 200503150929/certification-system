package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 课程-毕业要求指标点支撑矩阵批量保存请求
 */
@Setter
@Getter
public class CourseRequirementMatrixBatchRequest {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    private List<MatrixItem> items;

    @Setter
    @Getter
    public static class MatrixItem {

        @NotNull(message = "指标点ID不能为空")
        private Long indicatorId;

        @NotNull(message = "支撑等级不能为空")
        private String supportLevel; // H/M/L

    }
}