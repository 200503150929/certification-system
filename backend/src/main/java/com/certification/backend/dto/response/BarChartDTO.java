package com.certification.backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 柱状图数据项 DTO
 */
@Data
public class BarChartDTO {

    /** 名称（X轴标签，如指标点编号） */
    private String name;

    /** 数值（Y轴，如达成度） */
    private BigDecimal value;

    public static BarChartDTO of(String name, BigDecimal value) {
        BarChartDTO dto = new BarChartDTO();
        dto.setName(name);
        dto.setValue(value);
        return dto;
    }
}