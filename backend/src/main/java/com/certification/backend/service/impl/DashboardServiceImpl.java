package com.certification.backend.service.impl;

import com.certification.backend.dto.response.*;
import com.certification.backend.entity.*;
import com.certification.backend.repository.*;
import com.certification.backend.service.AchievementCalculationService;
import com.certification.backend.service.DashboardService;
import com.certification.backend.util.BigDecimalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据可视化看板业务实现
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    /** 雷达图六大维度与毕业要求编号前缀的映射 */
    private static final Map<String, String> DIMENSION_PREFIX_MAP = new LinkedHashMap<>();
    static {
        DIMENSION_PREFIX_MAP.put("工程知识", "1");
        DIMENSION_PREFIX_MAP.put("问题分析", "2");
        DIMENSION_PREFIX_MAP.put("设计/开发解决方案", "3");
        DIMENSION_PREFIX_MAP.put("研究", "4");
        DIMENSION_PREFIX_MAP.put("使用现代工具", "5");
        DIMENSION_PREFIX_MAP.put("个人和团队", "6");
    }

    /** 成绩区间定义 */
    private static final List<GradeRange> GRADE_RANGES = List.of(
            new GradeRange("90-100", "优秀", 90, 100),
            new GradeRange("80-89", "良好", 80, 89),
            new GradeRange("70-79", "中等", 70, 79),
            new GradeRange("60-69", "及格", 60, 69),
            new GradeRange("0-59", "不及格", 0, 59)
    );

    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository offeringRepository;
    private final IndicatorPointRepository indicatorRepository;
    private final GraduationRequirementRepository requirementRepository;
    private final AssessmentRepository assessmentRepository;
    private final GradeRepository gradeRepository;
    private final AchievementCalculationService achievementService;

    public DashboardServiceImpl(ProgramRepository programRepository,
                                 CourseRepository courseRepository,
                                 CourseOfferingRepository offeringRepository,
                                 IndicatorPointRepository indicatorRepository,
                                 GraduationRequirementRepository requirementRepository,
                                 AssessmentRepository assessmentRepository,
                                 GradeRepository gradeRepository,
                                 AchievementCalculationService achievementService) {
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.offeringRepository = offeringRepository;
        this.indicatorRepository = indicatorRepository;
        this.requirementRepository = requirementRepository;
        this.assessmentRepository = assessmentRepository;
        this.gradeRepository = gradeRepository;
        this.achievementService = achievementService;
    }

    // ==================== KPI 指标 ====================

    @Override
    public List<KpiDTO> getKpiData() {
        log.info("获取KPI指标数据");
        List<KpiDTO> result = new ArrayList<>();

        // 1. 培养方案版本数
        long programCount = programRepository.count();
        result.add(KpiDTO.of("培养方案版本数",
                new BigDecimal(programCount), "个",
                BigDecimal.ZERO, true));

        // 2. 课程总数
        long courseCount = courseRepository.count();
        result.add(KpiDTO.of("课程总数",
                new BigDecimal(courseCount), "门",
                BigDecimal.ZERO, true));

        // 3. 当前学期教学任务
        List<CourseOffering> allOfferings = offeringRepository.findAll();
        // 简化：取最新学年学期的开课数
        long currentOfferingCount = allOfferings.size();
        result.add(KpiDTO.of("当前学期教学任务",
                new BigDecimal(currentOfferingCount), "门次",
                BigDecimal.ZERO, true));

        // 4. 达成度达标率（所有指标点中达标的比例）
        List<IndicatorPoint> allIndicators = indicatorRepository.findAll();
        if (!allIndicators.isEmpty()) {
            int passedCount = 0;
            for (IndicatorPoint indicator : allIndicators) {
                IndicatorAchievementDTO dto = achievementService.calculateIndicatorAchievement(indicator.getId());
                if (dto != null && dto.isPassed()) {
                    passedCount++;
                }
            }
            BigDecimal passRate = BigDecimalUtil.divide(
                    new BigDecimal(passedCount),
                    new BigDecimal(allIndicators.size()));
            passRate = passRate.multiply(new BigDecimal("100"));
            result.add(KpiDTO.of("达成度达标率",
                    passRate, "%",
                    BigDecimal.ZERO, true));
        } else {
            result.add(KpiDTO.of("达成度达标率",
                    BigDecimal.ZERO, "%",
                    BigDecimal.ZERO, true));
        }

        return result;
    }

    // ==================== 柱状图 ====================

    @Override
    public List<BarChartDTO> getBarChartData(Long programId) {
        log.info("获取柱状图数据，programId={}", programId);

        List<BarChartDTO> result = new ArrayList<>();

        // 获取该培养方案下的所有毕业要求
        List<GraduationRequirement> requirements;
        if (programId != null) {
            requirements = requirementRepository.findByProgramId(programId);
        } else {
            requirements = requirementRepository.findAll();
        }

        // 遍历每个毕业要求下的每个指标点
        for (GraduationRequirement req : requirements) {
            List<IndicatorPoint> indicators = indicatorRepository.findByRequirementId(req.getId());
            for (IndicatorPoint indicator : indicators) {
                IndicatorAchievementDTO dto = achievementService.calculateIndicatorAchievement(indicator.getId());
                BigDecimal value = (dto != null) ? dto.getAchievement() : BigDecimal.ZERO;
                result.add(BarChartDTO.of(indicator.getCode(), value));
            }
        }

        return result;
    }

    // ==================== 雷达图 ====================

    @Override
    public RadarChartDTO getRadarChartData(Long programId) {
        log.info("获取雷达图数据，programId={}", programId);

        List<String> dimensionNames = new ArrayList<>(DIMENSION_PREFIX_MAP.keySet());
        List<BigDecimal> dimensionValues = new ArrayList<>();

        // 获取该培养方案下的所有毕业要求
        List<GraduationRequirement> requirements;
        if (programId != null) {
            requirements = requirementRepository.findByProgramId(programId);
        } else {
            requirements = requirementRepository.findAll();
        }

        // 按维度编号前缀分组
        Map<String, List<BigDecimal>> dimensionAchievements = new LinkedHashMap<>();
        for (String dimName : dimensionNames) {
            dimensionAchievements.put(dimName, new ArrayList<>());
        }

        for (GraduationRequirement req : requirements) {
            // 确定该毕业要求属于哪个维度
            String dimName = getDimensionByCode(req.getCode());
            if (dimName == null) continue;

            // 计算该毕业要求的达成度
            GraduationAchievementDTO gradDTO = achievementService.calculateGraduationAchievement(req.getId());
            if (gradDTO != null && gradDTO.getOverallAchievement() != null) {
                dimensionAchievements.get(dimName).add(gradDTO.getOverallAchievement());
            }
        }

        // 计算每个维度的平均达成度
        for (String dimName : dimensionNames) {
            List<BigDecimal> achievements = dimensionAchievements.get(dimName);
            if (achievements.isEmpty()) {
                dimensionValues.add(BigDecimal.ZERO);
            } else {
                dimensionValues.add(BigDecimalUtil.avg(achievements));
            }
        }

        RadarChartDTO dto = new RadarChartDTO();
        dto.setDimensions(dimensionNames);
        dto.setValues(dimensionValues);

        return dto;
    }

    /**
     * 根据毕业要求编号判断所属维度
     */
    private String getDimensionByCode(String code) {
        if (code == null) return null;
        for (Map.Entry<String, String> entry : DIMENSION_PREFIX_MAP.entrySet()) {
            if (code.startsWith(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    // ==================== 成绩分布 ====================

    @Override
    public List<GradeDistributionDTO> getGradeDistribution() {
        log.info("获取全院成绩分布");

        // 查询所有课程的平均成绩
        List<CourseOffering> offerings = offeringRepository.findAll();
        Map<Long, BigDecimal> courseAvgScores = new HashMap<>();

        for (CourseOffering offering : offerings) {
            List<Assessment> assessments = assessmentRepository.findByOfferingId(offering.getId());
            List<Long> assessmentIds = assessments.stream()
                    .map(Assessment::getId)
                    .collect(Collectors.toList());

            if (assessmentIds.isEmpty()) continue;

            List<Grade> grades = gradeRepository.findByAssessmentIdIn(assessmentIds);
            if (grades.isEmpty()) continue;

            BigDecimal avg = BigDecimalUtil.avg(
                    grades.stream().map(Grade::getScore).collect(Collectors.toList()));
            courseAvgScores.put(offering.getCourseId(), avg);
        }

        // 按分数区间统计
        Map<String, Integer> rangeCountMap = new LinkedHashMap<>();
        for (GradeRange range : GRADE_RANGES) {
            rangeCountMap.put(range.range, 0);
        }

        for (Map.Entry<Long, BigDecimal> entry : courseAvgScores.entrySet()) {
            double score = entry.getValue().doubleValue();
            for (GradeRange range : GRADE_RANGES) {
                if (score >= range.min && score <= range.max) {
                    rangeCountMap.merge(range.range, 1, Integer::sum);
                    break;
                }
            }
        }

        List<GradeDistributionDTO> result = new ArrayList<>();
        for (GradeRange range : GRADE_RANGES) {
            GradeDistributionDTO dto = new GradeDistributionDTO();
            dto.setRange(range.range);
            dto.setLabel(range.label);
            dto.setCount(rangeCountMap.getOrDefault(range.range, 0));
            result.add(dto);
        }

        return result;
    }

    // ==================== 考核环节得分率 ====================

    @Override
    public List<AssessmentScoreRateDTO> getAssessmentScoreRates() {
        log.info("获取考核环节平均得分率");

        // 查询所有考核环节
        List<Assessment> allAssessments = assessmentRepository.findAll();
        if (allAssessments.isEmpty()) {
            return Collections.emptyList();
        }

        // 按考核环节名称分组
        Map<String, List<BigDecimal>> nameScoreMap = new LinkedHashMap<>();

        for (Assessment assessment : allAssessments) {
            List<Grade> grades = gradeRepository.findByAssessmentId(assessment.getId());
            if (grades.isEmpty()) continue;

            BigDecimal avgScore = BigDecimalUtil.avg(
                    grades.stream().map(Grade::getScore).collect(Collectors.toList()));

            // 得分率 = 平均分 / 100
            BigDecimal scoreRate = BigDecimalUtil.scoreToAchievement(avgScore);

            nameScoreMap.computeIfAbsent(assessment.getName(), k -> new ArrayList<>())
                    .add(scoreRate);
        }

        List<AssessmentScoreRateDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> entry : nameScoreMap.entrySet()) {
            AssessmentScoreRateDTO dto = new AssessmentScoreRateDTO();
            dto.setName(entry.getKey());
            // 同名称的考核环节取平均得分率
            dto.setScoreRate(BigDecimalUtil.avg(entry.getValue()));
            result.add(dto);
        }

        return result;
    }

    // ==================== 内部类 ====================

    private static class GradeRange {
        final String range;
        final String label;
        final int min;
        final int max;

        GradeRange(String range, String label, int min, int max) {
            this.range = range;
            this.label = label;
            this.min = min;
            this.max = max;
        }
    }
}
