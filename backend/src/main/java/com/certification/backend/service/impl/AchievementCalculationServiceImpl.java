package com.certification.backend.service.impl;

import com.certification.backend.dto.response.*;
import com.certification.backend.entity.*;
import com.certification.backend.repository.*;
import com.certification.backend.service.AchievementCalculationService;
import com.certification.backend.util.BigDecimalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 达成度计算业务实现
 */
@Service
@Transactional(readOnly = true)
public class AchievementCalculationServiceImpl implements AchievementCalculationService {

    private static final Logger log = LoggerFactory.getLogger(AchievementCalculationServiceImpl.class);

    /** 支撑强度权重映射 */
    private static final Map<String, BigDecimal> SUPPORT_WEIGHT_MAP = Map.of(
            "H", new BigDecimal("1.0"),
            "M", new BigDecimal("0.7"),
            "L", new BigDecimal("0.4")
    );

    private final CourseObjectiveRepository objectiveRepository;
    private final AssessmentRepository assessmentRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final ObjectiveIndicatorMatrixRepository oiMatrixRepository;
    private final IndicatorPointRepository indicatorRepository;
    private final GraduationRequirementRepository requirementRepository;
    private final CourseOfferingRepository offeringRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseRequirementMatrixRepository crMatrixRepository;
    private final GradeWeightConfigRepository weightConfigRepository;

    public AchievementCalculationServiceImpl(
            CourseObjectiveRepository objectiveRepository,
            AssessmentRepository assessmentRepository,
            StudentGradeRepository studentGradeRepository,
            ObjectiveIndicatorMatrixRepository oiMatrixRepository,
            IndicatorPointRepository indicatorRepository,
            GraduationRequirementRepository requirementRepository,
            CourseOfferingRepository offeringRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseRequirementMatrixRepository crMatrixRepository,
            GradeWeightConfigRepository weightConfigRepository) {
        this.objectiveRepository = objectiveRepository;
        this.assessmentRepository = assessmentRepository;
        this.studentGradeRepository = studentGradeRepository;
        this.oiMatrixRepository = oiMatrixRepository;
        this.indicatorRepository = indicatorRepository;
        this.requirementRepository = requirementRepository;
        this.offeringRepository = offeringRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.crMatrixRepository = crMatrixRepository;
        this.weightConfigRepository = weightConfigRepository;
    }

    // ==================== 课程目标达成度计算 ====================

    @Override
    public List<ObjectiveAchievementDTO> calculateObjectiveAchievement(Long offeringId) {
        log.info("计算开课记录 [{}] 的课程目标达成度", offeringId);

        // 1. 查询该开课记录下的所有课程目标
        List<CourseObjective> objectives = objectiveRepository.findByOfferingId(offeringId);
        if (objectives.isEmpty()) {
            log.warn("开课记录 [{}] 下没有课程目标", offeringId);
            return Collections.emptyList();
        }

        // 2. 查询该开课记录下的所有学生成绩
        List<StudentGrade> studentGrades = studentGradeRepository.findByOfferingId(offeringId);
        if (studentGrades.isEmpty()) {
            log.warn("开课记录 [{}] 下没有学生成绩", offeringId);
            return Collections.emptyList();
        }

        // 3. 获取权重配置
        GradeWeightConfig config = weightConfigRepository.findByOfferingId(offeringId).orElse(null);
        BigDecimal dw = config != null ? config.getDailyWeight() : new BigDecimal("0.25");
        BigDecimal rw = config != null ? config.getReportWeight() : new BigDecimal("0.25");
        BigDecimal mw = config != null ? config.getMidtermWeight() : new BigDecimal("0.25");
        BigDecimal fw = config != null ? config.getFinalWeight() : new BigDecimal("0.25");

        // 4. 查询学生信息
        Set<Long> studentIds = studentGrades.stream()
                .map(StudentGrade::getStudentId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 5. 对每个课程目标计算达成度
        List<ObjectiveAchievementDTO> result = new ArrayList<>();

        for (CourseObjective objective : objectives) {
            ObjectiveAchievementDTO dto = calcSingleObjective(
                    objective, studentGrades, userMap, dw, rw, mw, fw);
            if (dto != null) {
                result.add(dto);
            }
        }

        log.info("开课记录 [{}] 课程目标达成度计算完成，共 {} 个目标", offeringId, result.size());
        return result;
    }

    /**
     * 计算单个课程目标的达成度
     */
    private ObjectiveAchievementDTO calcSingleObjective(
            CourseObjective objective,
            List<StudentGrade> studentGrades,
            Map<Long, User> userMap,
            BigDecimal dw, BigDecimal rw, BigDecimal mw, BigDecimal fw) {

        List<ObjectiveAchievementDTO.StudentAchievement> studentAchievements = new ArrayList<>();
        List<BigDecimal> achievementValues = new ArrayList<>();

        for (StudentGrade sg : studentGrades) {
            // 计算加权平均分
            BigDecimal weightedSum = BigDecimal.ZERO;
            BigDecimal totalWeight = BigDecimal.ZERO;

            if (sg.getDailyScore() != null) {
                weightedSum = weightedSum.add(sg.getDailyScore().multiply(dw));
                totalWeight = totalWeight.add(dw);
            }
            if (sg.getReportScore() != null) {
                weightedSum = weightedSum.add(sg.getReportScore().multiply(rw));
                totalWeight = totalWeight.add(rw);
            }
            if (sg.getMidtermScore() != null) {
                weightedSum = weightedSum.add(sg.getMidtermScore().multiply(mw));
                totalWeight = totalWeight.add(mw);
            }
            if (sg.getFinalScore() != null) {
                weightedSum = weightedSum.add(sg.getFinalScore().multiply(fw));
                totalWeight = totalWeight.add(fw);
            }

            if (totalWeight.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal avgScore = weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
            // 将百分制成绩转换为 0~1 之间的达成度
            BigDecimal achievement = BigDecimalUtil.scoreToAchievement(avgScore);

            ObjectiveAchievementDTO.StudentAchievement sa = new ObjectiveAchievementDTO.StudentAchievement();
            sa.setStudentId(sg.getStudentId());
            sa.setAchievement(BigDecimalUtil.format(achievement));

            User user = userMap.get(sg.getStudentId());
            if (user != null) {
                sa.setStudentName(user.getName());
                sa.setStudentNo(user.getUsername());
            }

            studentAchievements.add(sa);
            achievementValues.add(achievement);
        }

        if (achievementValues.isEmpty()) {
            return null;
        }

        // 班级达成度 = 各学生达成度的平均值
        BigDecimal classAchievement = BigDecimalUtil.avg(achievementValues);

        ObjectiveAchievementDTO dto = new ObjectiveAchievementDTO();
        dto.setObjectiveId(objective.getId());
        dto.setDescription(objective.getDescription());
        dto.setWeight(objective.getWeight());
        dto.setClassAchievement(classAchievement);
        dto.setAssessmentCount(4); // 四列成绩：平时、实验、期中、期末
        dto.setStudentCount(studentAchievements.size());
        dto.setStudentAchievements(studentAchievements);

        return dto;
    }

    // ==================== 指标点达成度计算 ====================

    @Override
    public IndicatorAchievementDTO calculateIndicatorAchievement(Long indicatorId) {
        return calculateIndicatorAchievementInternal(indicatorId, null);
    }

    @Override
    public IndicatorAchievementDTO calculateIndicatorAchievement(Long indicatorId, Set<Long> offeringIds) {
        return calculateIndicatorAchievementInternal(indicatorId, offeringIds);
    }

    /**
     * 计算指标点达成度的内部实现
     * @param indicatorId 指标点ID
     * @param offeringIds 限定开课记录ID集合，null或空表示不限
     */
    private IndicatorAchievementDTO calculateIndicatorAchievementInternal(Long indicatorId, Set<Long> offeringIds) {
        log.info("计算指标点 [{}] 的达成度，offeringIds={}", indicatorId, offeringIds);

        // 1. 查询指标点信息
        IndicatorPoint indicator = indicatorRepository.findById(indicatorId).orElse(null);
        if (indicator == null) {
            log.warn("指标点 [{}] 不存在", indicatorId);
            return null;
        }

        // 2. 查询所有支撑该指标点的课程目标
        List<ObjectiveIndicatorMatrix> matrices = oiMatrixRepository.findByIndicatorId(indicatorId);
        if (matrices.isEmpty()) {
            log.info("指标点 [{}] 没有支撑的课程目标", indicatorId);
            return buildEmptyIndicatorDTO(indicator);
        }

        // 2a. 如果指定了开课范围，过滤只保留属于这些开课的课程目标
        if (offeringIds != null && !offeringIds.isEmpty()) {
            Set<Long> allowedObjectiveIds = objectiveRepository
                    .findByOfferingIdIn(new ArrayList<>(offeringIds)).stream()
                    .map(CourseObjective::getId)
                    .collect(Collectors.toSet());
            matrices = matrices.stream()
                    .filter(m -> allowedObjectiveIds.contains(m.getObjectiveId()))
                    .collect(Collectors.toList());
        }

        if (matrices.isEmpty()) {
            log.info("指标点 [{}] 在指定范围内没有支撑的课程目标", indicatorId);
            return buildEmptyIndicatorDTO(indicator);
        }

        // 3. 对每个支撑的课程目标，计算其达成度
        List<IndicatorAchievementDTO.SupportingObjective> supportingList = new ArrayList<>();
        List<BigDecimal> weightedAchievements = new ArrayList<>();
        List<BigDecimal> supportWeights = new ArrayList<>();

        for (ObjectiveIndicatorMatrix matrix : matrices) {
            // 查询课程目标
            CourseObjective objective = objectiveRepository.findById(matrix.getObjectiveId()).orElse(null);
            if (objective == null) {
                continue;
            }

            // 计算该课程目标的达成度
            BigDecimal objAchievement = computeObjectiveAchievement(objective);
            if (objAchievement == null) {
                continue;
            }

            BigDecimal supportWeight = SUPPORT_WEIGHT_MAP.getOrDefault(
                    matrix.getSupportLevel(), new BigDecimal("0.5"));

            IndicatorAchievementDTO.SupportingObjective so = new IndicatorAchievementDTO.SupportingObjective();
            so.setObjectiveId(objective.getId());
            so.setDescription(objective.getDescription());
            so.setSupportLevel(matrix.getSupportLevel());
            so.setAchievement(objAchievement);
            supportingList.add(so);

            weightedAchievements.add(objAchievement.multiply(supportWeight));
            supportWeights.add(supportWeight);
        }

        // 4. 加权平均计算指标点达成度
        BigDecimal totalWeight = BigDecimalUtil.sum(supportWeights);
        BigDecimal indicatorAchievement;
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            indicatorAchievement = BigDecimal.ZERO;
        } else {
            indicatorAchievement = BigDecimalUtil.divide(
                    BigDecimalUtil.sum(weightedAchievements), totalWeight);
        }

        // 5. 判断是否达标
        BigDecimal passScore = indicator.getPassScore() != null
                ? indicator.getPassScore() : new BigDecimal("0.6");
        boolean passed = indicatorAchievement.compareTo(passScore) >= 0;

        IndicatorAchievementDTO dto = new IndicatorAchievementDTO();
        dto.setIndicatorId(indicator.getId());
        dto.setCode(indicator.getCode());
        dto.setDescription(indicator.getDescription());
        dto.setWeight(indicator.getWeight());
        dto.setPassScore(passScore);
        dto.setAchievement(indicatorAchievement);
        dto.setPassed(passed);
        dto.setSupportingObjectives(supportingList);

        return dto;
    }

    /**
     * 计算单个课程目标的达成度（从 StudentGrade 直接计算）
     * 返回 0~1 之间的达成度值
     */
    private BigDecimal computeObjectiveAchievement(CourseObjective objective) {
        Long offeringId = objective.getOfferingId();

        // 查询该开课下所有学生的 StudentGrade
        List<StudentGrade> grades = studentGradeRepository.findByOfferingId(offeringId);
        if (grades.isEmpty()) {
            return null;
        }

        // 获取权重配置
        GradeWeightConfig config = weightConfigRepository.findByOfferingId(offeringId).orElse(null);
        BigDecimal dw = config != null ? config.getDailyWeight() : new BigDecimal("0.25");
        BigDecimal rw = config != null ? config.getReportWeight() : new BigDecimal("0.25");
        BigDecimal mw = config != null ? config.getMidtermWeight() : new BigDecimal("0.25");
        BigDecimal fw = config != null ? config.getFinalWeight() : new BigDecimal("0.25");

        // 计算每位学生的加权平均分
        List<BigDecimal> studentAchievements = new ArrayList<>();
        for (StudentGrade sg : grades) {
            BigDecimal weightedSum = BigDecimal.ZERO;
            BigDecimal totalWeight = BigDecimal.ZERO;

            if (sg.getDailyScore() != null) {
                weightedSum = weightedSum.add(sg.getDailyScore().multiply(dw));
                totalWeight = totalWeight.add(dw);
            }
            if (sg.getReportScore() != null) {
                weightedSum = weightedSum.add(sg.getReportScore().multiply(rw));
                totalWeight = totalWeight.add(rw);
            }
            if (sg.getMidtermScore() != null) {
                weightedSum = weightedSum.add(sg.getMidtermScore().multiply(mw));
                totalWeight = totalWeight.add(mw);
            }
            if (sg.getFinalScore() != null) {
                weightedSum = weightedSum.add(sg.getFinalScore().multiply(fw));
                totalWeight = totalWeight.add(fw);
            }

            if (totalWeight.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal avgScore = weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
            studentAchievements.add(BigDecimalUtil.scoreToAchievement(avgScore));
        }

        if (studentAchievements.isEmpty()) {
            return null;
        }

        return BigDecimalUtil.avg(studentAchievements);
    }

    private IndicatorAchievementDTO buildEmptyIndicatorDTO(IndicatorPoint indicator) {
        IndicatorAchievementDTO dto = new IndicatorAchievementDTO();
        dto.setIndicatorId(indicator.getId());
        dto.setCode(indicator.getCode());
        dto.setDescription(indicator.getDescription());
        dto.setWeight(indicator.getWeight());
        dto.setPassScore(indicator.getPassScore());
        dto.setAchievement(BigDecimal.ZERO);
        dto.setPassed(false);
        dto.setSupportingObjectives(Collections.emptyList());
        return dto;
    }

    // ==================== 毕业要求达成度计算 ====================

    @Override
    public GraduationAchievementDTO calculateGraduationAchievement(Long requirementId) {
        return calculateGraduationAchievementInternal(requirementId, null);
    }

    @Override
    public GraduationAchievementDTO calculateGraduationAchievement(Long requirementId, Set<Long> offeringIds) {
        return calculateGraduationAchievementInternal(requirementId, offeringIds);
    }

    /**
     * 计算毕业要求达成度的内部实现
     * @param requirementId 毕业要求ID
     * @param offeringIds 限定开课记录ID集合，null或空表示不限
     */
    private GraduationAchievementDTO calculateGraduationAchievementInternal(Long requirementId, Set<Long> offeringIds) {
        log.info("计算毕业要求 [{}] 的达成度，offeringIds={}", requirementId, offeringIds);

        GraduationRequirement requirement = requirementRepository.findById(requirementId).orElse(null);
        if (requirement == null) {
            log.warn("毕业要求 [{}] 不存在", requirementId);
            return null;
        }

        // 查询该毕业要求下的所有指标点
        List<IndicatorPoint> indicators = indicatorRepository.findByRequirementId(requirementId);
        if (indicators.isEmpty()) {
            return buildEmptyGraduationDTO(requirement);
        }

        // 计算每个指标点的达成度
        List<GraduationAchievementDTO.IndicatorDetail> details = new ArrayList<>();
        BigDecimal minAchievement = BigDecimal.ONE;
        boolean allPassed = true;

        for (IndicatorPoint indicator : indicators) {
            IndicatorAchievementDTO indicatorDTO = calculateIndicatorAchievementInternal(indicator.getId(), offeringIds);

            GraduationAchievementDTO.IndicatorDetail detail = new GraduationAchievementDTO.IndicatorDetail();
            detail.setIndicatorId(indicator.getId());
            detail.setCode(indicator.getCode());
            detail.setDescription(indicator.getDescription());
            detail.setWeight(indicator.getWeight());
            detail.setPassScore(indicator.getPassScore());

            if (indicatorDTO != null) {
                detail.setAchievement(indicatorDTO.getAchievement());
                detail.setPassed(indicatorDTO.isPassed());
                if (indicatorDTO.getAchievement().compareTo(minAchievement) < 0) {
                    minAchievement = indicatorDTO.getAchievement();
                }
                if (!indicatorDTO.isPassed()) {
                    allPassed = false;
                }
            } else {
                detail.setAchievement(BigDecimal.ZERO);
                detail.setPassed(false);
                minAchievement = BigDecimal.ZERO;
                allPassed = false;
            }

            details.add(detail);
        }

        // 毕业要求达成度 = 各指标点达成度的最小值
        GraduationAchievementDTO dto = new GraduationAchievementDTO();
        dto.setRequirementId(requirement.getId());
        dto.setCode(requirement.getCode());
        dto.setDescription(requirement.getDescription());
        dto.setOverallAchievement(minAchievement);
        dto.setPassed(allPassed);
        dto.setIndicatorDetails(details);

        return dto;
    }

    private GraduationAchievementDTO buildEmptyGraduationDTO(GraduationRequirement requirement) {
        GraduationAchievementDTO dto = new GraduationAchievementDTO();
        dto.setRequirementId(requirement.getId());
        dto.setCode(requirement.getCode());
        dto.setDescription(requirement.getDescription());
        dto.setOverallAchievement(BigDecimal.ZERO);
        dto.setPassed(false);
        dto.setIndicatorDetails(Collections.emptyList());
        return dto;
    }

    // ==================== 评价报告生成 ====================

    @Override
    public AchievementReportDTO generateReport(Long offeringId) {
        log.info("生成开课记录 [{}] 的达成度评价报告", offeringId);

        CourseOffering offering = offeringRepository.findById(offeringId).orElse(null);
        if (offering == null) {
            log.warn("开课记录 [{}] 不存在", offeringId);
            return null;
        }

        Course course = courseRepository.findById(offering.getCourseId()).orElse(null);
        User teacher = userRepository.findById(offering.getTeacherId()).orElse(null);

        // 课程基本信息
        AchievementReportDTO.CourseInfo courseInfo = new AchievementReportDTO.CourseInfo();
        if (course != null) {
            courseInfo.setCourseId(course.getId());
            courseInfo.setCourseCode(course.getCode());
            courseInfo.setCourseName(course.getName());
            courseInfo.setCredits(course.getCredits());
        }
        courseInfo.setAcademicYear(offering.getAcademicYear());
        courseInfo.setSemester(offering.getSemester());

        // 教师信息
        AchievementReportDTO.TeacherInfo teacherInfo = new AchievementReportDTO.TeacherInfo();
        if (teacher != null) {
            teacherInfo.setTeacherId(teacher.getId());
            teacherInfo.setName(teacher.getName());
            teacherInfo.setCollege(teacher.getCollege());
        }

        // 课程目标达成度
        List<ObjectiveAchievementDTO> objectiveAchievements = calculateObjectiveAchievement(offeringId);

        // 学生人数
        int studentCount = objectiveAchievements.stream()
                .mapToInt(ObjectiveAchievementDTO::getStudentCount)
                .max()
                .orElse(0);

        // 毕业要求达成度（通过课程关联的培养方案找到毕业要求）
        List<GraduationAchievementDTO> gradAchievements = new ArrayList<>();
        if (course != null && course.getProgramId() != null) {
            List<GraduationRequirement> requirements = requirementRepository.findByProgramId(course.getProgramId());
            for (GraduationRequirement req : requirements) {
                GraduationAchievementDTO gradDTO = calculateGraduationAchievement(req.getId());
                if (gradDTO != null) {
                    gradAchievements.add(gradDTO);
                }
            }
        }

        // 考核结构信息（从 Assessment 获取考核环节配置）
        List<Assessment> assessments = assessmentRepository.findByOfferingId(offeringId);
        List<AchievementReportDTO.AssessmentStructure> structures = new ArrayList<>();

        // 查询课程目标名称映射
        Map<Long, String> objectiveNameMap = objectiveRepository.findByOfferingId(offeringId).stream()
                .collect(Collectors.toMap(CourseObjective::getId, CourseObjective::getDescription));

        // 从 StudentGrade 获取成绩数据计算各考核环节平均分
        List<StudentGrade> studentGrades = studentGradeRepository.findByOfferingId(offeringId);

        // 计算四列成绩的平均分
        BigDecimal avgDaily = BigDecimal.ZERO;
        BigDecimal avgReport = BigDecimal.ZERO;
        BigDecimal avgMidterm = BigDecimal.ZERO;
        BigDecimal avgFinal = BigDecimal.ZERO;
        int count = studentGrades.size();

        if (count > 0) {
            BigDecimal sumDaily = BigDecimal.ZERO;
            BigDecimal sumReport = BigDecimal.ZERO;
            BigDecimal sumMidterm = BigDecimal.ZERO;
            BigDecimal sumFinal = BigDecimal.ZERO;
            int dailyCount = 0, reportCount = 0, midtermCount = 0, finalCount = 0;

            for (StudentGrade sg : studentGrades) {
                if (sg.getDailyScore() != null) {
                    sumDaily = sumDaily.add(sg.getDailyScore());
                    dailyCount++;
                }
                if (sg.getReportScore() != null) {
                    sumReport = sumReport.add(sg.getReportScore());
                    reportCount++;
                }
                if (sg.getMidtermScore() != null) {
                    sumMidterm = sumMidterm.add(sg.getMidtermScore());
                    midtermCount++;
                }
                if (sg.getFinalScore() != null) {
                    sumFinal = sumFinal.add(sg.getFinalScore());
                    finalCount++;
                }
            }

            avgDaily = dailyCount > 0 ? sumDaily.divide(new BigDecimal(dailyCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            avgReport = reportCount > 0 ? sumReport.divide(new BigDecimal(reportCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            avgMidterm = midtermCount > 0 ? sumMidterm.divide(new BigDecimal(midtermCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            avgFinal = finalCount > 0 ? sumFinal.divide(new BigDecimal(finalCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        }

        // 为每个考核环节构建结构信息
        Map<String, Assessment> assessmentMap = assessments.stream()
                .collect(Collectors.toMap(Assessment::getName, a -> a, (a, b) -> a));

        // 四列成绩名称与平均分的映射
        Map<String, BigDecimal> nameToAvgScore = new LinkedHashMap<>();
        nameToAvgScore.put("平时作业", avgDaily);
        nameToAvgScore.put("实验报告", avgReport);
        nameToAvgScore.put("期中考试", avgMidterm);
        nameToAvgScore.put("期末考试", avgFinal);

        // 四列成绩名称与权重配置的映射
        Map<String, BigDecimal> nameToWeight = new LinkedHashMap<>();
        GradeWeightConfig config = weightConfigRepository.findByOfferingId(offeringId).orElse(null);
        nameToWeight.put("平时作业", config != null ? config.getDailyWeight() : new BigDecimal("0.25"));
        nameToWeight.put("实验报告", config != null ? config.getReportWeight() : new BigDecimal("0.25"));
        nameToWeight.put("期中考试", config != null ? config.getMidtermWeight() : new BigDecimal("0.25"));
        nameToWeight.put("期末考试", config != null ? config.getFinalWeight() : new BigDecimal("0.25"));

        for (Map.Entry<String, BigDecimal> entry : nameToAvgScore.entrySet()) {
            String name = entry.getKey();
            BigDecimal avgScore = entry.getValue();

            AchievementReportDTO.AssessmentStructure as = new AchievementReportDTO.AssessmentStructure();
            Assessment assessment = assessmentMap.get(name);
            if (assessment != null) {
                as.setAssessmentId(assessment.getId());
                as.setLinkedObjective(objectiveNameMap.getOrDefault(assessment.getObjectiveId(), "未关联"));
            }
            as.setName(name);
            as.setWeight(nameToWeight.getOrDefault(name, new BigDecimal("0.25")));
            as.setAverageScore(avgScore);
            as.setScoreRate(BigDecimalUtil.scoreToAchievement(avgScore));
            structures.add(as);
        }

        AchievementReportDTO report = new AchievementReportDTO();
        report.setCourseInfo(courseInfo);
        report.setTeacherInfo(teacherInfo);
        report.setStudentCount(studentCount);
        report.setObjectiveAchievements(objectiveAchievements);
        report.setGraduationAchievements(gradAchievements);
        report.setAssessmentStructures(structures);

        log.info("开课记录 [{}] 评价报告生成完成", offeringId);
        return report;
    }
}