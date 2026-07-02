package com.certification.backend.service;

import com.certification.backend.dto.response.*;

import java.util.List;

/**
 * 数据可视化看板业务接口
 */
public interface DashboardService {

    /**
     * 获取顶部KPI指标数据
     * 包含：培养方案版本数、课程总数、当前学期教学任务、达成度达标率
     * 附带环比变化趋势
     *
     * @param academicYear 筛选学年，null表示不限
     * @param semester     筛选学期，null表示不限
     */
    List<KpiDTO> getKpiData(String academicYear, String semester);

    /**
     * 获取毕业要求指标点达成度柱状图数据
     * X轴：各指标点编号，Y轴：达成度数值
     *
     * @param programId    培养方案ID，null表示全部
     * @param academicYear 筛选学年，null表示不限
     * @param semester     筛选学期，null表示不限
     */
    List<BarChartDTO> getBarChartData(Long programId, String academicYear, String semester);

    /**
     * 获取毕业要求六个维度雷达图数据
     * 维度：工程知识、问题分析、设计/开发解决方案、研究、个人和团队、使用现代工具
     *
     * @param programId    培养方案ID，null表示全部
     * @param academicYear 筛选学年，null表示不限
     * @param semester     筛选学期，null表示不限
     */
    RadarChartDTO getRadarChartData(Long programId, String academicYear, String semester);

    /**
     * 获取全院课程成绩分布
     * 按分数区间（90-100/80-89/70-79/60-69/0-59）统计课程数量
     *
     * @param academicYear 筛选学年，null表示不限
     * @param semester     筛选学期，null表示不限
     */
    List<GradeDistributionDTO> getGradeDistribution(String academicYear, String semester);

    /**
     * 获取各考核环节平均得分率
     *
     * @param academicYear 筛选学年，null表示不限
     * @param semester     筛选学期，null表示不限
     */
    List<AssessmentScoreRateDTO> getAssessmentScoreRates(String academicYear, String semester);

    /**
     * 获取学期选项列表（用于下拉选择器）
     */
    List<SemesterOptionDTO> getSemesterOptions();
}
