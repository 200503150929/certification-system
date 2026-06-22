package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 课程详情返回参数（继承课程基本信息，追加支撑矩阵列表）
 */
@Setter
@Getter
public class CourseDetailResponse extends CourseResponse {

    private List<MatrixItemResponse> matrixItems;

    @Setter
    @Getter
    public static class MatrixItemResponse {

        private Long id;
        private Long indicatorId;
        private String indicatorCode;
        private String indicatorDescription;
        private String supportLevel;

    }
}