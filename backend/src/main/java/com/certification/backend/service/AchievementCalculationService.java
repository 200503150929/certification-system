package com.certification.backend.service;

import com.certification.backend.dto.response.*;

import java.util.List;

/**
 * 达成度计算业务接口
 */
public interface AchievementCalculationService {

    /**
     * 计算指定开课记录的所有课程目标达成度
     * 包含学生个体达成度和班级整体达成度
     */
    List<ObjectiveAchievementDTO> calculateObjectiveAchievement(Long offeringId);

    /**
     * 计算指定指标点的达成度
     * 基于支撑该指标点的课程目标的达成度进行加权计算
     */
    IndicatorAchievementDTO calculateIndicatorAchievement(Long indicatorId);

    /**
     * 计算指定指标点的达成度（限定开课范围）
     * @param indicatorId 指标点ID
     * @param offeringIds 限定开课记录ID集合，null或空表示不限
     */
    IndicatorAchievementDTO calculateIndicatorAchievement(Long indicatorId, java.util.Set<Long> offeringIds);

    /**
     * 计算指定毕业要求的达成度
     * 取该毕业要求下所有指标点达成度的最小值
     */
    GraduationAchievementDTO calculateGraduationAchievement(Long requirementId);

    /**
     * 计算指定毕业要求的达成度（限定开课范围）
     * @param requirementId 毕业要求ID
     * @param offeringIds 限定开课记录ID集合，null或空表示不限
     */
    GraduationAchievementDTO calculateGraduationAchievement(Long requirementId, java.util.Set<Long> offeringIds);

    /**
     * 生成指定开课记录的达成度评价报告
     */
    AchievementReportDTO generateReport(Long offeringId);
}
