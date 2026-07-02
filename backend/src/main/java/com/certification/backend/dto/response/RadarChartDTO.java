package com.certification.backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 雷达图数据 DTO
 */
@Data
public class RadarChartDTO {

    /** 维度名称列表（如：工程知识、问题分析...） */
    private List<String> dimensions;

    /** 各维度对应的数值列表 */
    private List<BigDecimal> values;
}