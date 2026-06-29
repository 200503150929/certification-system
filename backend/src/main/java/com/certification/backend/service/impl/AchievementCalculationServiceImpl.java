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
    private final GradeRepository gradeRepository;
    private final ObjectiveIndicatorMatrixRepository oiMatrixRepository;
    private final IndicatorPointRepository indicatorRepository;
    private final GraduationRequirementRepository requirementRepository;
    private final CourseOfferingRepository offeringRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseRequirementMatrixRepository crMatrixRepository;

    public AchievementCalculationServiceImpl(
            CourseObjectiveRepository objectiveRepository,
            AssessmentRepository assessmentRepository,
            GradeRepository gradeRepository,
            ObjectiveIndicatorMatrixRepository oiMatrixRepository,
            IndicatorPointRepository indicatorRepository,
            GraduationRequirementRepository requirementRepository,
            CourseOfferingRepository offeringRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseRequirementMatrixRepository crMatrixRepository) {
        this.objectiveRepository = objectiveRepository;
        this.assessmentRepository = assessmentRepository;
        this.gradeRepository = gradeRepository;
        this.oiMatrixRepository = oiMatrixRepository;
        this.indicatorRepository = indicatorRepository;
        this.requirementRepository = requirementRepository;
        this.offeringRepository = offeringRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.crMatrixRepository = crMatrixRepository;
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

        // 2. 查询该开课记录下的所有考核环节
        List<Assessment> assessments = assessmentRepository.findByOfferingId(offeringId);
        Map<Long, List<Assessment>> objAssessmentMap = assessments.stream()
                .filter(a -> a.getObjectiveId() != null)
                .collect(Collectors.groupingBy(Assessment::getObjectiveId));

        // 3. 查询所有考核环节的成绩
        List<Long> assessmentIds = assessments.stream()
                .map(Assessment::getId)
                .collect(Collectors.toList());
        List<Grade> allGrades = assessmentIds.isEmpty()
                ? Collections.emptyList()
                : gradeRepository.findByAssessmentIdIn(assessmentIds);

        // 按 (assessmentId) 分组成绩
        Map<Long, List<Grade>> gradesByAssessment = allGrades.stream()
                .collect(Collectors.groupingBy(Grade::getAssessmentId));

        // 收集所有学生ID
        Set<Long> studentIds = allGrades.stream()
                .map(Grade::getStudentId)
                .collect(Collectors.toSet());

        // 查询学生信息
        Map<Long, User> userMap = userRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 4. 对每个课程目标计算达成度
        List<ObjectiveAchievementDTO> result = new ArrayList<>();
        for (CourseObjective objective : objectives) {
            ObjectiveAchievementDTO dto = calcSingleObjective(
                    objective, objAssessmentMap, gradesByAssessment, userMap);
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
            Map<Long, List<Assessment>> objAssessmentMap,
            Map<Long, List<Grade>> gradesByAssessment,
            Map<Long, User> userMap) {

        List<Assessment> linkedAssessments = objAssessmentMap.get(objective.getId());
        if (linkedAssessments == null || linkedAssessments.isEmpty()) {
            log.info("课程目标 [{}] 没有关联考核环节，跳过", objective.getId());
            return null;
        }

        // 收集该目标下所有考核环节的成绩
        Map<Long, Map<Long, BigDecimal>> studentScores = new HashMap<>();
        // studentId -> (assessmentId -> score)

        for (Assessment assessment : linkedAssessments) {
            List<Grade> grades = gradesByAssessment.getOrDefault(assessment.getId(), Collections.emptyList());
            for (Grade grade : grades) {
                studentScores
                        .computeIfAbsent(grade.getStudentId(), k -> new HashMap<>())
                        .put(assessment.getId(), grade.getScore());
            }
        }

        if (studentScores.isEmpty()) {
            log.info("课程目标 [{}] 没有成绩数据，跳过", objective.getId());
            return null;
        }

        // 计算每位学生的达成度
        List<ObjectiveAchievementDTO.StudentAchievement> studentAchievements = new ArrayList<>();
        List<BigDecimal> achievementValues = new ArrayList<>();

        // 计算考核环节权重之和（用于归一化）
        BigDecimal totalAssessmentWeight = linkedAssessments.stream()
                .map(a -> a.getWeight() != null ? a.getWeight() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAssessmentWeight.compareTo(BigDecimal.ZERO) == 0) {
            totalAssessmentWeight = BigDecimal.ONE;
        }

        for (Map.Entry<Long, Map<Long, BigDecimal>> entry : studentScores.entrySet()) {
            Long studentId = entry.getKey();
            Map<Long, BigDecimal> assessScoreMap = entry.getValue();

            // 加权求和：Σ(score_i × weight_i)
            BigDecimal weightedSum = BigDecimal.ZERO;
            for (Assessment assessment : linkedAssessments) {
                BigDecimal score = assessScoreMap.get(assessment.getId());
                if (score != null && assessment.getWeight() != null) {
                    weightedSum = weightedSum.add(
                            score.multiply(assessment.getWeight()));
                }
            }

            // 学生达成度 = 加权平均分 / 100
            BigDecimal studentAchievement = BigDecimalUtil.divide(weightedSum, totalAssessmentWeight);
            studentAchievement = BigDecimalUtil.scoreToAchievement(studentAchievement);

            ObjectiveAchievementDTO.StudentAchievement sa = new ObjectiveAchievementDTO.StudentAchievement();
            sa.setStudentId(studentId);
            sa.setAchievement(BigDecimalUtil.format(studentAchievement));

            User user = userMap.get(studentId);
            if (user != null) {
                sa.setStudentName(user.getName());
                sa.setStudentNo(user.getUsername());
            }

            studentAchievements.add(sa);
            achievementValues.add(studentAchievement);
        }

        // 班级达成度 = 各学生达成度的平均值
        BigDecimal classAchievement = BigDecimalUtil.avg(achievementValues);

        ObjectiveAchievementDTO dto = new ObjectiveAchievementDTO();
        dto.setObjectiveId(objective.getId());
        dto.setDescription(objective.getDescription());
        dto.setWeight(objective.getWeight());
        dto.setClassAchievement(classAchievement);
        dto.setAssessmentCount(linkedAssessments.size());
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
        IndicatorPoint indicator = indicatorRepository.findById(indicatorId)
                .orElse(null);
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

            // 计算该课程目标的达成度（使用开课记录级别的数据）
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
     * 计算单个课程目标的达成度（从考核环节和成绩直接计算）
     * 返回 0~1 之间的达成度值
     */
    private BigDecimal computeObjectiveAchievement(CourseObjective objective) {
        List<Assessment> assessments = assessmentRepository.findByOfferingId(objective.getOfferingId())
                .stream()
                .filter(a -> objective.getId().equals(a.getObjectiveId()))
                .collect(Collectors.toList());

        if (assessments.isEmpty()) {
            return null;
        }

        List<Long> assessmentIds = assessments.stream()
                .map(Assessment::getId)
                .collect(Collectors.toList());
        List<Grade> grades = gradeRepository.findByAssessmentIdIn(assessmentIds);

        if (grades.isEmpty()) {
            return null;
        }

        // 按学生分组
        Map<Long, List<Grade>> studentGrades = grades.stream()
                .collect(Collectors.groupingBy(Grade::getStudentId));

        BigDecimal totalWeight = assessments.stream()
                .map(a -> a.getWeight() != null ? a.getWeight() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            totalWeight = BigDecimal.ONE;
        }

        // 计算每位学生的加权平均分
        List<BigDecimal> studentAchievements = new ArrayList<>();
        for (Map.Entry<Long, List<Grade>> entry : studentGrades.entrySet()) {
            Map<Long, BigDecimal> scoreMap = entry.getValue().stream()
                    .collect(Collectors.toMap(Grade::getAssessmentId, Grade::getScore));

            BigDecimal weightedSum = BigDecimal.ZERO;
            for (Assessment assessment : assessments) {
                BigDecimal score = scoreMap.get(assessment.getId());
                if (score != null && assessment.getWeight() != null) {
                    weightedSum = weightedSum.add(score.multiply(assessment.getWeight()));
                }
            }

            BigDecimal avgScore = BigDecimalUtil.divide(weightedSum, totalWeight);
            studentAchievements.add(BigDecimalUtil.scoreToAchievement(avgScore));
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
            teacherInfo.setDepartment(teacher.getDepartment());
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

        // 考核结构信息
        List<Assessment> assessments = assessmentRepository.findByOfferingId(offeringId);
        List<AchievementReportDTO.AssessmentStructure> structures = new ArrayList<>();

        // 查询课程目标名称映射
        Map<Long, String> objectiveNameMap = objectiveRepository.findByOfferingId(offeringId).stream()
                .collect(Collectors.toMap(CourseObjective::getId, CourseObjective::getDescription));

        List<Long> assessmentIds = assessments.stream().map(Assessment::getId).collect(Collectors.toList());
        List<Grade> allGrades = assessmentIds.isEmpty()
                ? Collections.emptyList()
                : gradeRepository.findByAssessmentIdIn(assessmentIds);
        Map<Long, List<Grade>> gradeMap = allGrades.stream()
                .collect(Collectors.groupingBy(Grade::getAssessmentId));

        for (Assessment assessment : assessments) {
            AchievementReportDTO.AssessmentStructure as = new AchievementReportDTO.AssessmentStructure();
            as.setAssessmentId(assessment.getId());
            as.setName(assessment.getName());
            as.setWeight(assessment.getWeight());
            as.setLinkedObjective(objectiveNameMap.getOrDefault(assessment.getObjectiveId(), "未关联"));

            List<Grade> grades = gradeMap.getOrDefault(assessment.getId(), Collections.emptyList());
            if (!grades.isEmpty()) {
                BigDecimal avgScore = BigDecimalUtil.avg(
                        grades.stream().map(Grade::getScore).collect(Collectors.toList()));
                as.setAverageScore(avgScore);
                as.setScoreRate(BigDecimalUtil.divide(avgScore, BigDecimalUtil.FULL_SCORE));
            } else {
                as.setAverageScore(BigDecimal.ZERO);
                as.setScoreRate(BigDecimal.ZERO);
            }

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
