package com.certification.backend.service.impl;

import com.certification.backend.dto.request.GradeRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.GradeResponse;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.entity.Assessment;
import com.certification.backend.entity.CourseOffering;
import com.certification.backend.entity.Grade;
import com.certification.backend.entity.User;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.AssessmentRepository;
import com.certification.backend.repository.CourseOfferingRepository;
import com.certification.backend.repository.GradeRepository;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.service.GradeService;
import com.certification.backend.util.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成绩管理业务实现
 */
@Service
@Transactional
public class GradeServiceImpl implements GradeService {

    private static final Logger log = LoggerFactory.getLogger(GradeServiceImpl.class);

    private final GradeRepository gradeRepository;
    private final AssessmentRepository assessmentRepository;
    private final CourseOfferingRepository offeringRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BigDecimal SCORE_MIN = BigDecimal.ZERO;
    private static final BigDecimal SCORE_MAX = new BigDecimal("100");

    public GradeServiceImpl(GradeRepository gradeRepository,
                            AssessmentRepository assessmentRepository,
                            CourseOfferingRepository offeringRepository,
                            UserRepository userRepository) {
        this.gradeRepository = gradeRepository;
        this.assessmentRepository = assessmentRepository;
        this.offeringRepository = offeringRepository;
        this.userRepository = userRepository;
    }

    @Override
    public GradeResponse add(GradeRequest request, String username) {
        validateScore(request.getScore());

        CourseOffering offering = getOfferingAndValidateOwner(request.getOfferingId(), username);

        // 校验考核环节是否存在且属于该开课记录
        Assessment assessment = assessmentRepository.findById(request.getAssessmentId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "考核环节不存在"));
        if (!assessment.getOfferingId().equals(request.getOfferingId())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "该考核环节不属于此开课记录");
        }

        Grade grade = new Grade();
        grade.setAssessmentId(request.getAssessmentId());
        grade.setStudentId(request.getStudentId());
        grade.setScore(request.getScore());

        Grade saved = gradeRepository.save(grade);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeResponse> listByOfferingId(Long offeringId, String username) {
        getOfferingAndValidateOwner(offeringId, username);

        // 查询该开课记录下的所有考核环节
        List<Assessment> assessments = assessmentRepository.findByOfferingId(offeringId);
        if (assessments.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有考核环节ID，批量查询成绩
        List<Long> assessmentIds = assessments.stream()
                .map(Assessment::getId)
                .collect(Collectors.toList());
        List<Grade> grades = gradeRepository.findByAssessmentIdIn(assessmentIds);

        // 构建 assessmentId -> assessmentName 的映射
        Map<Long, String> assessmentNameMap = assessments.stream()
                .collect(Collectors.toMap(Assessment::getId, Assessment::getName));

        return grades.stream()
                .map(g -> toResponse(g, assessmentNameMap.getOrDefault(g.getAssessmentId(), "")))
                .collect(Collectors.toList());
    }

    @Override
    public GradeResponse update(Long id, GradeRequest request, String username) {
        validateScore(request.getScore());

        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "成绩记录不存在"));

        // 通过考核环节找到开课记录，校验教师权限
        Assessment assessment = assessmentRepository.findById(grade.getAssessmentId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "考核环节不存在"));
        getOfferingAndValidateOwner(assessment.getOfferingId(), username);

        grade.setScore(request.getScore());

        Grade saved = gradeRepository.save(grade);
        return toResponse(saved);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验成绩是否在 0-100 之间
     */
    private void validateScore(BigDecimal score) {
        if (score.compareTo(SCORE_MIN) < 0 || score.compareTo(SCORE_MAX) > 0) {
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

    private GradeResponse toResponse(Grade grade) {
        return toResponse(grade, "");
    }

    private GradeResponse toResponse(Grade grade, String assessmentName) {
        GradeResponse resp = new GradeResponse();
        resp.setId(grade.getId());
        resp.setAssessmentId(grade.getAssessmentId());
        resp.setAssessmentName(assessmentName);
        resp.setStudentId(grade.getStudentId());
        resp.setScore(grade.getScore());
        resp.setCreatedAt(grade.getCreatedAt() != null ? grade.getCreatedAt().format(DTF) : null);

        // 查询学生姓名
        userRepository.findById(grade.getStudentId())
                .ifPresent(u -> resp.setStudentName(u.getName()));

        return resp;
    }

    // ==================== 成绩批量导入 ====================

    @Override
    @Transactional
    public int importGrades(Long assessmentId, List<ExcelUtil.GradeImportRow> gradeRows, String teacherUsername) {
        // 校验考核环节存在，并校验教师权限
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "考核环节不存在"));
        getOfferingAndValidateOwner(assessment.getOfferingId(), teacherUsername);

        int successCount = 0;
        int updateCount = 0;
        int insertCount = 0;

        for (ExcelUtil.GradeImportRow row : gradeRows) {
            var studentOpt = userRepository.findByUsername(row.getStudentNo());
            if (studentOpt.isEmpty()) {
                log.warn("成绩导入跳过：学号 '{}' 在系统中找不到对应学生", row.getStudentNo());
                continue;
            }

            User student = studentOpt.get();

            // 先查询是否已存在该学生在此考核环节的成绩，存在则更新，不存在则新增（与 saveOrUpdate 行为一致）
            var existing = gradeRepository.findByAssessmentIdAndStudentId(assessmentId, student.getId());
            Grade grade;
            if (existing.isPresent()) {
                grade = existing.get();
                grade.setScore(row.getScore());
                updateCount++;
                log.debug("成绩导入更新：assessmentId={}，studentId={}，新分数={}", assessmentId, student.getId(), row.getScore());
            } else {
                grade = new Grade();
                grade.setAssessmentId(assessmentId);
                grade.setStudentId(student.getId());
                grade.setScore(row.getScore());
                insertCount++;
                log.debug("成绩导入新增：assessmentId={}，studentId={}，分数={}", assessmentId, student.getId(), row.getScore());
            }

            gradeRepository.save(grade);
            successCount++;
        }

        log.info("成绩导入完成：考核环节ID={}，成功导入 {} / {} 条（新增 {} 条，更新 {} 条）",
                assessmentId, successCount, gradeRows.size(), insertCount, updateCount);
        return successCount;
    }

    // ==================== 保存/更新成绩 ====================

    @Override
    @Transactional
    public GradeResponse saveOrUpdate(GradeRequest request, String username) {
        validateScore(request.getScore());

        // 校验开课记录并校验教师权限
        getOfferingAndValidateOwner(request.getOfferingId(), username);

        // 校验考核环节存在且属于该开课记录
        Assessment assessment = assessmentRepository.findById(request.getAssessmentId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "考核环节不存在"));
        if (!assessment.getOfferingId().equals(request.getOfferingId())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "该考核环节不属于此开课记录");
        }

        Grade grade;

        if (request.getId() != null) {
            // 按 ID 更新
            grade = gradeRepository.findById(request.getId())
                    .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "成绩记录不存在"));
            grade.setScore(request.getScore());
            log.info("成绩更新：ID={}，新分数={}", grade.getId(), request.getScore());
        } else {
            // 按 (assessmentId + studentId) 查找：存在则更新，不存在则插入
            var existing = gradeRepository.findByAssessmentIdAndStudentId(
                    request.getAssessmentId(), request.getStudentId());
            if (existing.isPresent()) {
                grade = existing.get();
                grade.setScore(request.getScore());
                log.info("成绩已存在，更新：assessmentId={}，studentId={}，新分数={}",
                        request.getAssessmentId(), request.getStudentId(), request.getScore());
            } else {
                grade = new Grade();
                grade.setAssessmentId(request.getAssessmentId());
                grade.setStudentId(request.getStudentId());
                grade.setScore(request.getScore());
                log.info("成绩新增：assessmentId={}，studentId={}，分数={}",
                        request.getAssessmentId(), request.getStudentId(), request.getScore());
            }
        }

        Grade saved = gradeRepository.save(grade);
        return toResponse(saved, assessment.getName());
    }

    // ==================== 分页查询成绩列表 ====================

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradeResponse> listGrades(Long offeringId, String studentName,
                                                PageQuery pageQuery, String username) {
        getOfferingAndValidateOwner(offeringId, username);

        // 查询该开课记录下的所有考核环节
        List<Assessment> assessments = assessmentRepository.findByOfferingId(offeringId);
        if (assessments.isEmpty()) {
            return new PageResult<>(0, pageQuery.getPageNum(), pageQuery.getPageSize(), Collections.emptyList());
        }

        List<Long> assessmentIds = assessments.stream()
                .map(Assessment::getId)
                .collect(Collectors.toList());
        List<Grade> grades = gradeRepository.findByAssessmentIdIn(assessmentIds);

        if (grades.isEmpty()) {
            return new PageResult<>(0, pageQuery.getPageNum(), pageQuery.getPageSize(), Collections.emptyList());
        }

        // 构建 assessmentId -> assessmentName 映射
        Map<Long, String> assessmentNameMap = assessments.stream()
                .collect(Collectors.toMap(Assessment::getId, Assessment::getName));

        // 批量查询所有学生信息（避免 N+1）
        List<Long> studentIds = grades.stream()
                .map(Grade::getStudentId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> studentMap = userRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 组装所有成绩响应（含学生姓名 + 考核环节名称）
        List<GradeResponse> allResponses = grades.stream()
                .map(g -> {
                    GradeResponse resp = new GradeResponse();
                    resp.setId(g.getId());
                    resp.setAssessmentId(g.getAssessmentId());
                    resp.setAssessmentName(assessmentNameMap.getOrDefault(g.getAssessmentId(), ""));
                    resp.setStudentId(g.getStudentId());
                    resp.setScore(g.getScore());
                    resp.setCreatedAt(g.getCreatedAt() != null ? g.getCreatedAt().format(DTF) : null);
                    User student = studentMap.get(g.getStudentId());
                    if (student != null) {
                        resp.setStudentName(student.getName());
                    }
                    return resp;
                })
                .collect(Collectors.toList());

        // 按学生姓名模糊过滤
        if (studentName != null && !studentName.trim().isEmpty()) {
            String keyword = studentName.trim().toLowerCase();
            allResponses = allResponses.stream()
                    .filter(r -> r.getStudentName() != null
                            && r.getStudentName().toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
        }

        // 内存分页
        long total = allResponses.size();
        int pageNum = pageQuery.getPageNum();
        int pageSize = pageQuery.getPageSize();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allResponses.size());

        List<GradeResponse> pageList = fromIndex < allResponses.size()
                ? allResponses.subList(fromIndex, toIndex)
                : Collections.emptyList();

        return new PageResult<>(total, pageNum, pageSize, pageList);
    }

    // ==================== 删除成绩 ====================

    @Override
    @Transactional
    public void deleteGrade(Long id, String username) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "成绩记录不存在"));

        // 通过考核环节找到开课记录，校验教师权限
        Assessment assessment = assessmentRepository.findById(grade.getAssessmentId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "考核环节不存在"));
        getOfferingAndValidateOwner(assessment.getOfferingId(), username);

        gradeRepository.delete(grade);
        log.info("成绩删除：ID={}，assessmentId={}，studentId={}", id, grade.getAssessmentId(), grade.getStudentId());
    }

    // ==================== 成绩导出 ====================

    @Override
    @Transactional(readOnly = true)
    public void exportGrades(Long offeringId, String username, HttpServletResponse response) throws java.io.IOException {
        getOfferingAndValidateOwner(offeringId, username);

        // 查询该开课记录下的所有考核环节
        List<Assessment> assessments = assessmentRepository.findByOfferingId(offeringId);
        if (assessments.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "该开课记录下无考核环节，无法导出");
        }

        // 获取所有考核环节ID，批量查询成绩
        List<Long> assessmentIds = assessments.stream()
                .map(Assessment::getId)
                .collect(Collectors.toList());
        List<Grade> grades = gradeRepository.findByAssessmentIdIn(assessmentIds);

        if (grades.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "该开课记录下无成绩数据，无法导出");
        }

        // 构建 assessmentId -> assessmentName 映射
        Map<Long, String> assessmentNameMap = assessments.stream()
                .collect(Collectors.toMap(Assessment::getId, Assessment::getName));

        // 批量查询所有学生信息（减少数据库查询次数）
        List<Long> studentIds = grades.stream()
                .map(Grade::getStudentId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> studentMap = userRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 组装导出行数据
        List<ExcelUtil.GradeExportRow> exportRows = new ArrayList<>();
        for (Grade grade : grades) {
            User student = studentMap.get(grade.getStudentId());
            String studentNo = student != null ? student.getUsername() : "未知";
            String studentName = student != null ? student.getName() : "未知";
            String assessmentName = assessmentNameMap.getOrDefault(grade.getAssessmentId(), "未知");

            exportRows.add(new ExcelUtil.GradeExportRow(
                    studentNo,
                    studentName,
                    assessmentName,
                    grade.getScore() != null ? grade.getScore().toPlainString() : ""
            ));
        }

        // 生成文件名并导出
        String fileName = "成绩表_" + offeringId;
        log.info("成绩导出：开课ID={}，共导出 {} 条成绩", offeringId, exportRows.size());
        ExcelUtil.writeGradesToExcel(exportRows, response, fileName);
    }
}
