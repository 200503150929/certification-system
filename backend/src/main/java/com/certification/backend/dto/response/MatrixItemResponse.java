package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 课程目标-指标点矩阵项返回参数
 */
@Setter
@Getter
public class MatrixItemResponse {

    private Long id;
    private Long objectiveId;
    private String objectiveDescription;
    private Long indicatorId;
    private String indicatorCode;
    private String indicatorDesc;
    private String supportLevel;
}
