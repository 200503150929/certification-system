package com.certification.backend.service.impl;

import com.certification.backend.dto.request.GradeRequest;
import com.certification.backend.dto.response.GradeResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
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
}
