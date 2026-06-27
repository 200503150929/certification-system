package com.certification.backend.service.impl;

import com.certification.backend.dto.request.GradeWeightRequest;
import com.certification.backend.dto.request.StudentGradeBatchRequest;
import com.certification.backend.dto.response.GradeWeightResponse;
import com.certification.backend.dto.response.StudentGradeResponse;
import com.certification.backend.entity.*;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.*;
import com.certification.backend.service.StudentGradeService;
import com.certification.backend.util.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生成绩服务实现（四列固定成绩：平时成绩、实验报告、期中考试、期末考试）
 */
@Service
@Transactional
public class StudentGradeServiceImpl implements StudentGradeService {

    private static final Logger log = LoggerFactory.getLogger(StudentGradeServiceImpl.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BigDecimal SCORE_MIN = BigDecimal.ZERO;
    private static final BigDecimal SCORE_MAX = new BigDecimal("100");

    /** 四列标准成绩名称，用于与 Assessment 表匹配同步 */
    private static final List<String> ASSESSMENT_NAMES = List.of("平时成绩", "实验报告", "期中考试", "期末考试");

    private final StudentGradeRepository studentGradeRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final CourseOfferingRepository offeringRepository;
    private final UserRepository userRepository;
    private final AssessmentRepository assessmentRepository;
    private final GradeRepository gradeRepository;
    private final GradeWeightConfigRepository weightConfigRepository;

    public StudentGradeServiceImpl(StudentGradeRepository studentGradeRepository,
                                   StudentCourseRepository studentCourseRepository,
                                   CourseOfferingRepository offeringRepository,
                                   UserRepository userRepository,
                                   AssessmentRepository assessmentRepository,
                                   GradeRepository gradeRepository,
                                   GradeWeightConfigRepository weightConfigRepository) {
        this.studentGradeRepository = studentGradeRepository;
        this.studentCourseRepository = studentCourseRepository;
        this.offeringRepository = offeringRepository;
        this.userRepository = userRepository;
        this.assessmentRepository = assessmentRepository;
        this.gradeRepository = gradeRepository;
        this.weightConfigRepository = weightConfigRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentGradeResponse> listByOffering(Long offeringId, String username) {
        getOfferingAndValidateOwner(offeringId, username);

        // 1. 查询所有选课学生
        List<StudentCourse> enrollments = studentCourseRepository.findByOfferingId(offeringId);
        if (enrollments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> enrolledStudentIds = enrollments.stream()
                .map(StudentCourse::getStudentId)
                .distinct()
                .collect(Collectors.toList());

        // 2. 批量查询学生信息
        Map<Long, User> studentMap = userRepository.findAllById(enrolledStudentIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 3. 查询已有的成绩记录
        List<StudentGrade> grades = studentGradeRepository.findByOfferingId(offeringId);
        Map<Long, StudentGrade> gradeMap = grades.stream()
                .collect(Collectors.toMap(StudentGrade::getStudentId, g -> g, (a, b) -> a));

        // 4. 为每个选课学生组装响应（未录入成绩的也包含在内）
        List<StudentGradeResponse> result = new ArrayList<>();
        for (Long studentId : enrolledStudentIds) {
            User student = studentMap.get(studentId);
            if (student == null) continue;

            StudentGradeResponse resp = new StudentGradeResponse();
            resp.setOfferingId(offeringId);
            resp.setStudentId(studentId);
            resp.setStudentNo(student.getUsername());
            resp.setStudentName(student.getName());

            StudentGrade grade = gradeMap.get(studentId);
            if (grade != null) {
                resp.setId(grade.getId());
                resp.setDailyScore(grade.getDailyScore());
                resp.setReportScore(grade.getReportScore());
                resp.setMidtermScore(grade.getMidtermScore());
                resp.setFinalScore(grade.getFinalScore());
                resp.setTotalScore(grade.getTotalScore());
                resp.setCreatedAt(grade.getCreatedAt() != null ? grade.getCreatedAt().format(DTF) : null);
                resp.setUpdatedAt(grade.getUpdatedAt() != null ? grade.getUpdatedAt().format(DTF) : null);
            }

            result.add(resp);
        }

        // 按学号排序
        result.sort(Comparator.comparing(StudentGradeResponse::getStudentNo, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    @Override
    @Transactional
    public List<StudentGradeResponse> saveBatch(StudentGradeBatchRequest request, String username) {
        CourseOffering offering = getOfferingAndValidateOwner(request.getOfferingId(), username);

        List<StudentGrade> toSave = new ArrayList<>();

        for (StudentGradeBatchRequest.GradeItem item : request.getGrades()) {
            // 校验分数范围
            validateScore(item.getDailyScore());
            validateScore(item.getReportScore());
            validateScore(item.getMidtermScore());
            validateScore(item.getFinalScore());

            // 查找已有记录或新建
            StudentGrade sg = studentGradeRepository
                    .findByOfferingIdAndStudentId(request.getOfferingId(), item.getStudentId())
                    .orElseGet(() -> {
                        StudentGrade newSg = new StudentGrade();
                        newSg.setOfferingId(request.getOfferingId());
                        newSg.setStudentId(item.getStudentId());
                        return newSg;
                    });

            sg.setDailyScore(item.getDailyScore());
            sg.setReportScore(item.getReportScore());
            sg.setMidtermScore(item.getMidtermScore());
            sg.setFinalScore(item.getFinalScore());
            sg.setTotalScore(computeTotalScore(request.getOfferingId(), item.getDailyScore(), item.getReportScore(),
                    item.getMidtermScore(), item.getFinalScore()));

            toSave.add(sg);
        }

        List<StudentGrade> saved = studentGradeRepository.saveAll(toSave);
        log.info("批量保存学生成绩：开课ID={}，保存 {} 条记录", request.getOfferingId(), saved.size());

        // 同步到旧 Grade 表（与 Assessment 匹配的列）
        Map<Long, StudentGrade> savedMap = saved.stream()
                .collect(Collectors.toMap(StudentGrade::getStudentId, sg -> sg, (a, b) -> a));
        syncToGradeTable(request.getOfferingId(), savedMap);

        return listByOffering(request.getOfferingId(), username);
    }

    @Override
    @Transactional
    public int importGrades(Long offeringId, MultipartFile file, String username) {
        getOfferingAndValidateOwner(offeringId, username);

        List<ExcelUtil.StudentGradeImportRow> importRows = ExcelUtil.readStudentGradesFromExcel(file);
        if (importRows.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Excel 文件中无有效数据");
        }

        // 构建 StudentGradeBatchRequest
        StudentGradeBatchRequest request = new StudentGradeBatchRequest();
        request.setOfferingId(offeringId);
        List<StudentGradeBatchRequest.GradeItem> items = new ArrayList<>();

        for (ExcelUtil.StudentGradeImportRow row : importRows) {
            User student = userRepository.findByUsername(row.getStudentNo()).orElse(null);
            if (student == null) {
                log.warn("成绩导入跳过：学号 '{}' 在系统中找不到对应学生", row.getStudentNo());
                continue;
            }

            StudentGradeBatchRequest.GradeItem item = new StudentGradeBatchRequest.GradeItem();
            item.setStudentId(student.getId());
            item.setDailyScore(row.getDailyScore());
            item.setReportScore(row.getReportScore());
            item.setMidtermScore(row.getMidtermScore());
            item.setFinalScore(row.getFinalScore());
            items.add(item);
        }

        if (items.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Excel 文件中无匹配的学生");
        }

        request.setGrades(items);
        List<StudentGradeResponse> responses = saveBatch(request, username);
        log.info("Excel 批量导入学生成绩完成：开课ID={}，导入 {} 条", offeringId, responses.size());
        return responses.size();
    }

    @Override
    @Transactional(readOnly = true)
    public void exportGrades(Long offeringId, String username, HttpServletResponse response) throws java.io.IOException {
        List<StudentGradeResponse> list = listByOffering(offeringId, username);
        if (list.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "该开课记录下无学生成绩数据，无法导出");
        }

        List<ExcelUtil.StudentGradeExportRow> exportRows = list.stream()
                .map(r -> new ExcelUtil.StudentGradeExportRow(
                        r.getStudentNo(),
                        r.getStudentName(),
                        r.getDailyScore() != null ? r.getDailyScore().toPlainString() : "",
                        r.getReportScore() != null ? r.getReportScore().toPlainString() : "",
                        r.getMidtermScore() != null ? r.getMidtermScore().toPlainString() : "",
                        r.getFinalScore() != null ? r.getFinalScore().toPlainString() : "",
                        r.getTotalScore() != null ? r.getTotalScore().toPlainString() : ""
                ))
                .collect(Collectors.toList());

        String fileName = "学生成绩表_" + offeringId;
        log.info("学生成绩导出：开课ID={}，共导出 {} 条记录", offeringId, exportRows.size());
        ExcelUtil.writeStudentGradesToExcel(exportRows, response, fileName);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentGradeResponse getStudentGrade(Long offeringId, String username) {
        // 验证学生存在
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.UNAUTHORIZED, "用户不存在"));

        // 验证学生选修了该课程
        studentCourseRepository.findByOfferingId(offeringId).stream()
                .filter(sc -> sc.getStudentId().equals(student.getId()))
                .findAny()
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.FORBIDDEN, "未选修该课程"));

        // 查询成绩记录
        StudentGrade grade = studentGradeRepository.findByOfferingIdAndStudentId(offeringId, student.getId())
                .orElse(null);

        StudentGradeResponse resp = new StudentGradeResponse();
        resp.setOfferingId(offeringId);
        resp.setStudentId(student.getId());
        resp.setStudentNo(student.getUsername());
        resp.setStudentName(student.getName());

        if (grade != null) {
            resp.setId(grade.getId());
            resp.setDailyScore(grade.getDailyScore());
            resp.setReportScore(grade.getReportScore());
            resp.setMidtermScore(grade.getMidtermScore());
            resp.setFinalScore(grade.getFinalScore());
            resp.setTotalScore(grade.getTotalScore());
            resp.setCreatedAt(grade.getCreatedAt() != null ? grade.getCreatedAt().format(DTF) : null);
            resp.setUpdatedAt(grade.getUpdatedAt() != null ? grade.getUpdatedAt().format(DTF) : null);
        }

        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public GradeWeightResponse getWeightConfig(Long offeringId, String username) {
        getOfferingAndValidateOwner(offeringId, username);
        GradeWeightConfig config = weightConfigRepository.findByOfferingId(offeringId).orElse(null);
        return toWeightResponse(config, offeringId);
    }

    @Override
    @Transactional
    public GradeWeightResponse saveWeightConfig(GradeWeightRequest request, String username) {
        getOfferingAndValidateOwner(request.getOfferingId(), username);

        GradeWeightConfig config = weightConfigRepository.findByOfferingId(request.getOfferingId())
                .orElseGet(() -> {
                    GradeWeightConfig newConfig = new GradeWeightConfig();
                    newConfig.setOfferingId(request.getOfferingId());
                    return newConfig;
                });

        if (request.getDailyWeight() != null) config.setDailyWeight(request.getDailyWeight());
        if (request.getReportWeight() != null) config.setReportWeight(request.getReportWeight());
        if (request.getMidtermWeight() != null) config.setMidtermWeight(request.getMidtermWeight());
        if (request.getFinalWeight() != null) config.setFinalWeight(request.getFinalWeight());

        GradeWeightConfig saved = weightConfigRepository.save(config);
        log.info("权重配置已保存：开课ID={}", request.getOfferingId());
        return toWeightResponse(saved, request.getOfferingId());
    }

    private GradeWeightResponse toWeightResponse(GradeWeightConfig config, Long offeringId) {
        GradeWeightResponse resp = new GradeWeightResponse();
        resp.setOfferingId(offeringId);
        if (config != null) {
            resp.setId(config.getId());
            resp.setDailyWeight(config.getDailyWeight());
            resp.setReportWeight(config.getReportWeight());
            resp.setMidtermWeight(config.getMidtermWeight());
            resp.setFinalWeight(config.getFinalWeight());
            resp.setCreatedAt(config.getCreatedAt() != null ? config.getCreatedAt().format(DTF) : null);
            resp.setUpdatedAt(config.getUpdatedAt() != null ? config.getUpdatedAt().format(DTF) : null);
        } else {
            // 返回默认权重
            resp.setDailyWeight(new BigDecimal("0.2500"));
            resp.setReportWeight(new BigDecimal("0.2500"));
            resp.setMidtermWeight(new BigDecimal("0.2500"));
            resp.setFinalWeight(new BigDecimal("0.2500"));
        }
        return resp;
    }

    // ==================== 私有方法 ====================

    /**
     * 计算最终成绩：加权平均（使用该开课的权重配置，默认各 25%），保留两位小数
     */
    private BigDecimal computeTotalScore(Long offeringId, BigDecimal daily, BigDecimal report, BigDecimal midterm, BigDecimal finalExam) {
        // 获取权重配置
        BigDecimal dw = new BigDecimal("0.25");
        BigDecimal rw = new BigDecimal("0.25");
        BigDecimal mw = new BigDecimal("0.25");
        BigDecimal fw = new BigDecimal("0.25");

        GradeWeightConfig config = weightConfigRepository.findByOfferingId(offeringId).orElse(null);
        if (config != null) {
            dw = config.getDailyWeight();
            rw = config.getReportWeight();
            mw = config.getMidtermWeight();
            fw = config.getFinalWeight();
        }

        // 加权求和 / 有效权重之和
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        if (daily != null) {
            weightedSum = weightedSum.add(daily.multiply(dw));
            totalWeight = totalWeight.add(dw);
        }
        if (report != null) {
            weightedSum = weightedSum.add(report.multiply(rw));
            totalWeight = totalWeight.add(rw);
        }
        if (midterm != null) {
            weightedSum = weightedSum.add(midterm.multiply(mw));
            totalWeight = totalWeight.add(mw);
        }
        if (finalExam != null) {
            weightedSum = weightedSum.add(finalExam.multiply(fw));
            totalWeight = totalWeight.add(fw);
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }

    /**
     * 校验单个分数是否在 0-100 范围内
     */
    private void validateScore(BigDecimal score) {
        if (score != null && (score.compareTo(SCORE_MIN) < 0 || score.compareTo(SCORE_MAX) > 0)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "成绩必须在0-100之间");
        }
    }

    /**
     * 获取开课记录并校验当前用户是否为该记录的授课教师
     */
    private CourseOffering getOfferingAndValidateOwner(Long offeringId, String username) {
        User teacher = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.UNAUTHORIZED, "用户不存在"));

        CourseOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "开课记录不存在"));

        if (!offering.getTeacherId().equals(teacher.getId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无权操作该开课记录");
        }

        return offering;
    }

    /**
     * 将 StudentGrade 数据同步到旧的 Grade 表
     * 仅同步名称匹配的考核环节（平时成绩、实验报告、期中考试、期末考试）
     * 不存在的考核环节静默跳过（教师可以在考核管理中单独配置）
     */
    private void syncToGradeTable(Long offeringId, Map<Long, StudentGrade> savedGrades) {
        List<Assessment> assessments = assessmentRepository.findByOfferingId(offeringId);
        if (assessments.isEmpty()) {
            log.debug("开课 ID={} 下无考核环节，跳过 Grade 表同步", offeringId);
            return;
        }

        // 按名称匹配：assessmentName（去空白小写）-> Assessment
        Map<String, Assessment> nameToAssessment = new LinkedHashMap<>();
        for (Assessment a : assessments) {
            if (a.getName() != null) {
                nameToAssessment.putIfAbsent(a.getName().trim().toLowerCase(), a);
            }
        }

        for (String standardName : ASSESSMENT_NAMES) {
            Assessment matched = nameToAssessment.get(standardName.toLowerCase());
            if (matched == null) {
                log.debug("开课 ID={} 下无名为 '{}' 的考核环节，跳过同步", offeringId, standardName);
                continue;
            }

            for (StudentGrade sg : savedGrades.values()) {
                BigDecimal score = switch (standardName) {
                    case "平时成绩" -> sg.getDailyScore();
                    case "实验报告" -> sg.getReportScore();
                    case "期中考试" -> sg.getMidtermScore();
                    case "期末考试" -> sg.getFinalScore();
                    default -> null;
                };

                if (score == null) continue;

                // 按 (assessmentId + studentId) 查找已有 Grade 记录，存在则更新，不存在则插入
                var existing = gradeRepository.findByAssessmentIdAndStudentId(matched.getId(), sg.getStudentId());
                Grade grade;
                if (existing.isPresent()) {
                    grade = existing.get();
                    grade.setScore(score);
                } else {
                    grade = new Grade();
                    grade.setAssessmentId(matched.getId());
                    grade.setStudentId(sg.getStudentId());
                    grade.setScore(score);
                }
                gradeRepository.save(grade);
            }
        }

        log.info("Grade 表同步完成：开课 ID={}，考生 {} 人", offeringId, savedGrades.size());
    }
}
