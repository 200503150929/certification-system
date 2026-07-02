package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 指标点信息（含所属毕业要求），供教师端课程目标-指标点矩阵使用
 */
@Setter
@Getter
public class IndicatorInfoResponse {

    private Long indicatorId;       // 指标点ID
    private String indicatorCode;   // 指标点编号
    private String indicatorDesc;   // 指标点描述
    private Long requirementId;     // 所属毕业要求ID
    private String requirementCode; // 毕业要求编号
}
