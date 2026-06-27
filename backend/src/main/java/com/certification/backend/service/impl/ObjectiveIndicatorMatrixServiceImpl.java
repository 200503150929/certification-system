package com.certification.backend.service.impl;

import com.certification.backend.dto.request.MatrixItemRequest;
import com.certification.backend.dto.response.IndicatorInfoResponse;
import com.certification.backend.dto.response.MatrixItemResponse;
import com.certification.backend.entity.*;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.*;
import com.certification.backend.service.ObjectiveIndicatorMatrixService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程目标-指标点支撑矩阵业务实现
 */
@Service
@Transactional
public class ObjectiveIndicatorMatrixServiceImpl implements ObjectiveIndicatorMatrixService {

    /** 支撑强度常量 */
    public static final String SUPPORT_HIGH = "H";
    public static final String SUPPORT_MEDIUM = "M";
    public static final String SUPPORT_LOW = "L";

    private final ObjectiveIndicatorMatrixRepository matrixRepository;
    private final CourseObjectiveRepository objectiveRepository;
    private final CourseOfferingRepository offeringRepository;
    private final IndicatorPointRepository indicatorPointRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final GraduationRequirementRepository requirementRepository;

    public ObjectiveIndicatorMatrixServiceImpl(ObjectiveIndicatorMatrixRepository matrixRepository,
                                               CourseObjectiveRepository objectiveRepository,
                                               CourseOfferingRepository offeringRepository,
                                               IndicatorPointRepository indicatorPointRepository,
                                               UserRepository userRepository,
                                               CourseRepository courseRepository,
                                               GraduationRequirementRepository requirementRepository) {
        this.matrixRepository = matrixRepository;
        this.objectiveRepository = objectiveRepository;
        this.offeringRepository = offeringRepository;
        this.indicatorPointRepository = indicatorPointRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.requirementRepository = requirementRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatrixItemResponse> getMatrix(Long offeringId, String username) {
        getOfferingAndValidateOwner(offeringId, username);

        // 查询该开课记录下的所有课程目标
        List<CourseObjective> objectives = objectiveRepository.findByOfferingId(offeringId);
        if (objectives.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有课程目标ID，批量查询矩阵关系
        List<Long> objectiveIds = objectives.stream()
                .map(CourseObjective::getId)
                .collect(Collectors.toList());

        List<ObjectiveIndicatorMatrix> matrices = matrixRepository.findByObjectiveIdIn(objectiveIds);

        // 构建 courseObjectiveId -> description 映射
        Map<Long, String> objectiveDescMap = objectives.stream()
                .collect(Collectors.toMap(CourseObjective::getId, CourseObjective::getDescription));

        // 批量查询所有关联的指标点信息
        List<Long> indicatorIds = matrices.stream()
                .map(ObjectiveIndicatorMatrix::getIndicatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, IndicatorPoint> indicatorMap = indicatorIds.isEmpty()
                ? Map.of()
                : indicatorPointRepository.findAllById(indicatorIds).stream()
                    .collect(Collectors.toMap(IndicatorPoint::getId, ip -> ip));

        // 组装响应
        return matrices.stream()
                .map(m -> toResponse(m, objectiveDescMap, indicatorMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<MatrixItemResponse> batchSaveMatrix(Long offeringId, List<MatrixItemRequest> items, String username) {
        getOfferingAndValidateOwner(offeringId, username);

        // 查询该开课记录下的所有课程目标
        List<CourseObjective> objectives = objectiveRepository.findByOfferingId(offeringId);
        if (objectives.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "该开课记录下无课程目标");
        }

        List<Long> objectiveIds = objectives.stream()
                .map(CourseObjective::getId)
                .collect(Collectors.toList());

        // 校验提交的所有 objectiveId 是否属于该开课记录
        for (MatrixItemRequest item : items) {
            if (!objectiveIds.contains(item.getObjectiveId())) {
                throw new BusinessException(ResultCodeEnum.BAD_REQUEST,
                        "课程目标ID " + item.getObjectiveId() + " 不属于此开课记录");
            }
        }

        // 先删除该开课记录下所有课程目标的旧矩阵关系
        matrixRepository.deleteByObjectiveIdIn(objectiveIds);

        // 批量插入新的矩阵关系
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        List<ObjectiveIndicatorMatrix> newMatrices = items.stream()
                .map(item -> {
                    ObjectiveIndicatorMatrix matrix = new ObjectiveIndicatorMatrix();
                    matrix.setObjectiveId(item.getObjectiveId());
                    matrix.setIndicatorId(item.getIndicatorId());
                    matrix.setSupportLevel(item.getSupportLevel());
                    return matrix;
                })
                .collect(Collectors.toList());

        List<ObjectiveIndicatorMatrix> savedMatrices = matrixRepository.saveAll(newMatrices);

        // 构建映射用于响应
        Map<Long, String> objectiveDescMap = objectives.stream()
                .collect(Collectors.toMap(CourseObjective::getId, CourseObjective::getDescription));

        List<Long> indicatorIds = savedMatrices.stream()
                .map(ObjectiveIndicatorMatrix::getIndicatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, IndicatorPoint> indicatorMap = indicatorPointRepository.findAllById(indicatorIds).stream()
                .collect(Collectors.toMap(IndicatorPoint::getId, ip -> ip));

        return savedMatrices.stream()
                .map(m -> toResponse(m, objectiveDescMap, indicatorMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndicatorInfoResponse> getIndicatorsByOffering(Long offeringId, String username) {
        CourseOffering offering = getOfferingAndValidateOwner(offeringId, username);

        // offering → course → programId
        Course course = courseRepository.findById(offering.getCourseId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "课程不存在"));
        Long programId = course.getProgramId();

        // 获取该专业的所有毕业要求
        List<GraduationRequirement> requirements = requirementRepository.findByProgramId(programId);
        if (requirements.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有指标点
        List<Long> reqIds = requirements.stream().map(GraduationRequirement::getId).collect(Collectors.toList());
        List<IndicatorPoint> allIndicators = indicatorPointRepository.findByRequirementIdIn(reqIds);
        if (allIndicators.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建 requirementId → requirementCode 映射
        Map<Long, String> reqCodeMap = requirements.stream()
                .collect(Collectors.toMap(GraduationRequirement::getId, GraduationRequirement::getCode));

        return allIndicators.stream().map(ip -> {
            IndicatorInfoResponse resp = new IndicatorInfoResponse();
            resp.setIndicatorId(ip.getId());
            resp.setIndicatorCode(ip.getCode());
            resp.setIndicatorDesc(ip.getDescription());
            resp.setRequirementId(ip.getRequirementId());
            resp.setRequirementCode(reqCodeMap.getOrDefault(ip.getRequirementId(), ""));
            return resp;
        }).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

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

    private MatrixItemResponse toResponse(ObjectiveIndicatorMatrix matrix,
                                          Map<Long, String> objectiveDescMap,
                                          Map<Long, IndicatorPoint> indicatorMap) {
        MatrixItemResponse resp = new MatrixItemResponse();
        resp.setId(matrix.getId());
        resp.setObjectiveId(matrix.getObjectiveId());
        resp.setObjectiveDescription(objectiveDescMap.getOrDefault(matrix.getObjectiveId(), ""));
        resp.setIndicatorId(matrix.getIndicatorId());
        resp.setSupportLevel(matrix.getSupportLevel());

        IndicatorPoint ip = indicatorMap.get(matrix.getIndicatorId());
        if (ip != null) {
            resp.setIndicatorCode(ip.getCode());
            resp.setIndicatorDesc(ip.getDescription());
        }

        return resp;
    }
}
