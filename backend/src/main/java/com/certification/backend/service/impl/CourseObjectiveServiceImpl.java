package com.certification.backend.service.impl;

import com.certification.backend.dto.request.CourseObjectiveRequest;
import com.certification.backend.dto.response.CourseObjectiveResponse;
import com.certification.backend.entity.CourseObjective;
import com.certification.backend.entity.CourseOffering;
import com.certification.backend.entity.StudentCourse;
import com.certification.backend.entity.User;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.AssessmentRepository;
import com.certification.backend.repository.CourseObjectiveRepository;
import com.certification.backend.repository.CourseOfferingRepository;
import com.certification.backend.repository.StudentCourseRepository;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.service.CourseObjectiveService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程目标管理业务实现
 */
@Service
@Transactional
public class CourseObjectiveServiceImpl implements CourseObjectiveService {

    private final CourseObjectiveRepository objectiveRepository;
    private final CourseOfferingRepository offeringRepository;
    private final UserRepository userRepository;
    private final AssessmentRepository assessmentRepository;
    private final StudentCourseRepository studentCourseRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CourseObjectiveServiceImpl(CourseObjectiveRepository objectiveRepository,
                                      CourseOfferingRepository offeringRepository,
                                      UserRepository userRepository,
                                      AssessmentRepository assessmentRepository,
                                      StudentCourseRepository studentCourseRepository) {
        this.objectiveRepository = objectiveRepository;
        this.offeringRepository = offeringRepository;
        this.userRepository = userRepository;
        this.assessmentRepository = assessmentRepository;
        this.studentCourseRepository = studentCourseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseObjectiveResponse> listByOfferingId(Long offeringId, String username) {
        CourseOffering offering = getOfferingAndValidateOwner(offeringId, username);
        List<CourseObjective> objectives = objectiveRepository.findByOfferingId(offeringId);
        return objectives.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseObjectiveResponse> listByOfferingIdForStudent(Long offeringId, String username) {
        // 验证学生存在且已选修该课程
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.UNAUTHORIZED, "用户不存在"));
        List<StudentCourse> enrollments = studentCourseRepository.findByOfferingId(offeringId);
        boolean enrolled = enrollments.stream().anyMatch(sc -> sc.getStudentId().equals(student.getId()));
        if (!enrolled) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "未选修该课程");
        }
        List<CourseObjective> objectives = objectiveRepository.findByOfferingId(offeringId);
        return objectives.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public CourseObjectiveResponse add(Long offeringId, CourseObjectiveRequest request, String username) {
        CourseOffering offering = getOfferingAndValidateOwner(offeringId, username);

        CourseObjective objective = new CourseObjective();
        objective.setOfferingId(offeringId);
        // 自动生成编号（如未提供）：查找该开课下已有目标数，生成 CO1, CO2, ...
        objective.setCode(generateCode(offeringId, request.getCode()));
        objective.setDescription(request.getDescription());
        objective.setWeight(request.getWeight());

        CourseObjective saved = objectiveRepository.save(objective);
        return toResponse(saved);
    }

    @Override
    public CourseObjectiveResponse update(Long id, CourseObjectiveRequest request, String username) {
        CourseObjective objective = objectiveRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "课程目标不存在"));

        // 校验该目标所属的开课记录是否属于当前教师
        getOfferingAndValidateOwner(objective.getOfferingId(), username);

        if (request.getCode() != null) {
            objective.setCode(request.getCode());
        }
        if (request.getDescription() != null) {
            objective.setDescription(request.getDescription());
        }
        if (request.getWeight() != null) {
            objective.setWeight(request.getWeight());
        }

        CourseObjective saved = objectiveRepository.save(objective);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, String username) {
        CourseObjective objective = objectiveRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "课程目标不存在"));

        // 校验该目标所属的开课记录是否属于当前教师
        getOfferingAndValidateOwner(objective.getOfferingId(), username);

        // 删除保护：如果目标已关联考核环节，则禁止删除
        if (assessmentRepository.existsByObjectiveId(id)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "该课程目标已关联考核环节，无法删除");
        }

        objectiveRepository.deleteById(id);
    }

    // ==================== 私有方法 ====================

    /**
     * 自动生成课程目标编号（如 CO1, CO2, ...）
     * 如果用户已提供编号则直接使用，否则在该开课下自动递增
     */
    private String generateCode(Long offeringId, String providedCode) {
        if (providedCode != null && !providedCode.isBlank()) {
            return providedCode.trim();
        }
        List<CourseObjective> existing = objectiveRepository.findByOfferingId(offeringId);
        int maxNum = 0;
        for (CourseObjective obj : existing) {
            String code = obj.getCode();
            if (code != null && code.startsWith("CO")) {
                try {
                    int num = Integer.parseInt(code.substring(2));
                    if (num > maxNum) maxNum = num;
                } catch (NumberFormatException ignored) { }
            }
        }
        return "CO" + (maxNum + 1);
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

    private CourseObjectiveResponse toResponse(CourseObjective objective) {
        CourseObjectiveResponse resp = new CourseObjectiveResponse();
        resp.setId(objective.getId());
        resp.setOfferingId(objective.getOfferingId());
        resp.setCode(objective.getCode());
        resp.setDescription(objective.getDescription());
        resp.setWeight(objective.getWeight());
        resp.setCreatedAt(objective.getCreatedAt() != null ? objective.getCreatedAt().format(DTF) : null);
        return resp;
    }
}
