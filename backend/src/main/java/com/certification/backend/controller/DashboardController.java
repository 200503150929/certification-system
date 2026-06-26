package com.certification.backend.controller;

import com.certification.backend.dto.response.*;
import com.certification.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据可视化看板控制器
 */
@Tag(name = "10-数据可视化看板", description = "KPI指标、达成度柱状图、雷达图、成绩分布、考核得分率")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "获取KPI指标数据",
            description = "返回培养方案版本数、课程总数、当前学期教学任务、达成度达标率四个指标及环比变化")
    @GetMapping("/kpi")
    public ResponseVO<List<KpiDTO>> getKpiData() {
        return ResponseVO.success(dashboardService.getKpiData());
    }

    @Operation(summary = "获取指标点达成度柱状图数据",
            description = "返回各指标点编号及达成度值，用于柱状图横向对比")
    @GetMapping("/bar-chart")
    public ResponseVO<List<BarChartDTO>> getBarChartData(
            @RequestParam(required = false) Long programId) {
        return ResponseVO.success(dashboardService.getBarChartData(programId));
    }

    @Operation(summary = "获取毕业要求雷达图数据",
            description = "返回工程知识、问题分析、设计/开发解决方案、研究、个人和团队、使用现代工具六个维度的达成度")
    @GetMapping("/radar-chart")
    public ResponseVO<RadarChartDTO> getRadarChartData(
            @RequestParam(required = false) Long programId) {
        return ResponseVO.success(dashboardService.getRadarChartData(programId));
    }

    @Operation(summary = "获取全院课程成绩分布",
            description = "按90-100/80-89/70-79/60-69/0-59五个区间统计课程数量分布")
    @GetMapping("/grade-distribution")
    public ResponseVO<List<GradeDistributionDTO>> getGradeDistribution() {
        return ResponseVO.success(dashboardService.getGradeDistribution());
    }

    @Operation(summary = "获取各考核环节平均得分率",
            description = "统计各考核环节（平时作业、期中考试、期末考试等）的平均得分率")
    @GetMapping("/assessment-score-rates")
    public ResponseVO<List<AssessmentScoreRateDTO>> getAssessmentScoreRates() {
        return ResponseVO.success(dashboardService.getAssessmentScoreRates());
    }
}