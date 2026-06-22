package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 毕业要求详情（含下属指标点列表）
 */
@Setter
@Getter
public class GraduationRequirementDetailResponse {

    private Long id;
    private Long programId;
    private String code;
    private String description;
    private String createdAt;
    private List<IndicatorPointResponse> indicatorPoints;

}