package com.certification.backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * KPI指标数据 DTO
 */
@Data
public class KpiDTO {

    /** 指标名称 */
    private String name;

    /** 当前数值 */
    private BigDecimal value;

    /** 单位 */
    private String unit;

    /** 环比变化百分比（正数=上升，负数=下降） */
    private BigDecimal changePercent;

    /** 是否正向指标（上升代表好转） */
    private boolean positive;

    public static KpiDTO of(String name, BigDecimal value, String unit,
                             BigDecimal changePercent, boolean positive) {
        KpiDTO dto = new KpiDTO();
        dto.setName(name);
        dto.setValue(value);
        dto.setUnit(unit);
        dto.setChangePercent(changePercent);
        dto.setPositive(positive);
        return dto;
    }
}
