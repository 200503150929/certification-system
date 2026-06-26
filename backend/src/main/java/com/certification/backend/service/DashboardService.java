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
     */
    List<KpiDTO> getKpiData();

    /**
     * 获取毕业要求指标点达成度柱状图数据
     * X轴：各指标点编号，Y轴：达成度数值
     */
    List<BarChartDTO> getBarChartData(Long programId);

    /**
     * 获取毕业要求六个维度雷达图数据
     * 维度：工程知识、问题分析、设计/开发解决方案、研究、个人和团队、使用现代工具
     */
    RadarChartDTO getRadarChartData(Long programId);

    /**
     * 获取全院课程成绩分布
     * 按分数区间（90-100/80-89/70-79/60-69/0-59）统计课程数量
     */
    List<GradeDistributionDTO> getGradeDistribution();

    /**
     * 获取各考核环节平均得分率
     */
    List<AssessmentScoreRateDTO> getAssessmentScoreRates();
}
