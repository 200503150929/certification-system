package com.certification.backend.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学期选项 DTO（用于看板学期下拉选择器）
 */
@Data
@NoArgsConstructor
public class SemesterOptionDTO {

    /** 学年，如 "2025-2026" */
    private String academicYear;

    /** 学期，如 "第1学期" */
    private String semester;

    /** 前端展示文本，如 "2025-2026 第1学期" */
    private String label;

    /** 前端选中值，如 "2025-2026_第1学期"，选中后拆分传参 */
    private String value;

    /**
     * JPQL 构造器 — 由 findDistinctSemesters() 查询使用
     */
    public SemesterOptionDTO(String academicYear, String semester) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.label = academicYear + " " + semester;
        this.value = academicYear + "_" + semester;
    }
}
