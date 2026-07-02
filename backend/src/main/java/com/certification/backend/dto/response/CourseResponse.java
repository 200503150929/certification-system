package com.certification.backend.dto.response;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 课程列表项返回参数
 */
@Setter
@Getter
public class CourseResponse {

    @ExcelIgnore
    private Long id;
    @ExcelIgnore
    private Long programId;
    @ExcelIgnore
    private String createdAt;

    // ========== 以下字段会导出到 Excel ==========

    @ExcelProperty("课程代码")
    @ColumnWidth(15)
    private String code;

    @ExcelProperty("课程名称")
    @ColumnWidth(25)
    private String name;

    @ExcelProperty("学分")
    @ColumnWidth(10)
    private BigDecimal credits;

    @ExcelProperty("总学时")
    @ColumnWidth(10)
    private Integer totalHours;

    @ExcelProperty("理论学时")
    @ColumnWidth(10)
    private Integer theoryHours;

    @ExcelProperty("实验学时")
    @ColumnWidth(10)
    private Integer labHours;

    @ExcelProperty("开课学期")
    @ColumnWidth(18)
    private String semester;

    @ExcelProperty("课程类型")
    @ColumnWidth(15)
    private String courseType;

    @ExcelProperty("是否必修")
    @ColumnWidth(10)
    private String isRequiredDisplay;

    @ExcelProperty("培养方案")
    @ColumnWidth(20)
    private String programName;

    /**
     * 设置是否必修的显示文本（供 EasyExcel 导出使用）
     */
    public void setIsRequired(Boolean isRequired) {
        this.isRequiredDisplay = isRequired != null && isRequired ? "是" : "否";
    }
}