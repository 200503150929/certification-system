package com.certification.backend.service.impl;

import com.certification.backend.dto.request.AssessmentRequest;
import com.certification.backend.dto.response.AssessmentResponse;
import com.certification.backend.entity.Assessment;
import com.certification.backend.entity.CourseOffering;
import com.certification.backend.entity.User;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.AssessmentRepository;
import com.certification.backend.repository.CourseOfferingRepository;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.service.AssessmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 考核管理业务实现
 */
@Service
@Transactional
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final CourseOfferingRepository offeringRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BigDecimal MAX_WEIGHT_TOTAL = BigDecimal.ONE;

    public AssessmentServiceImpl(AssessmentRepository assessmentRepository,
                                 CourseOfferingRepository offeringRepository,
                                 UserRepository userRepository) {
        this.assessmentRepository = assessmentRepository;
        this.offeringRepository = offeringRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<AssessmentResponse> add(AssessmentRequest request, String username) {
        CourseOffering offering = getOfferingAndValidateOwner(request.getOfferingId(), username);

        // 校验权重总和：已有考核方式的权重 + 本次新增的权重不能超过 1.0
        List<Assessment> existing = assessmentRepository.findByOfferingId(request.getOfferingId());
        BigDecimal currentTotal = existing.stream()
                .map(Assessment::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newTotal = currentTotal.add(request.getWeight());
        if (newTotal.compareTo(MAX_WEIGHT_TOTAL) > 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST,
                    "考核方式权重总和不能超过1.0，当前已有权重总和为" + currentTotal + "，本次新增权重为" + request.getWeight());
        }

        // 按关联的课程目标ID逐个创建考核记录
        List<Assessment> createdList = new ArrayList<>();
        for (Long objectiveId : request.getObjectiveIds()) {
            Assessment assessment = new Assessment();
            assessment.setOfferingId(request.getOfferingId());
            assessment.setName(request.getAssessmentName());
            assessment.setWeight(request.getWeight());
            assessment.setObjectiveId(objectiveId);
            createdList.add(assessmentRepository.save(assessment));
        }

        return createdList.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentResponse> listByOfferingId(Long offeringId, String username) {
        getOfferingAndValidateOwner(offeringId, username);
        List<Assessment> assessments = assessmentRepository.findByOfferingId(offeringId);
        return assessments.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id, String username) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "考核方式不存在"));

        // 校验该考核方式所属的开课记录是否属于当前教师
        getOfferingAndValidateOwner(assessment.getOfferingId(), username);

        assessmentRepository.deleteById(id);
    }

    // ==================== 私有方法 ====================

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

    private AssessmentResponse toResponse(Assessment assessment) {
        AssessmentResponse resp = new AssessmentResponse();
        resp.setId(assessment.getId());
        resp.setOfferingId(assessment.getOfferingId());
        resp.setName(assessment.getName());
        resp.setWeight(assessment.getWeight());
        resp.setObjectiveId(assessment.getObjectiveId());
        resp.setCreatedAt(assessment.getCreatedAt() != null ? assessment.getCreatedAt().format(DTF) : null);
        return resp;
    }
}
