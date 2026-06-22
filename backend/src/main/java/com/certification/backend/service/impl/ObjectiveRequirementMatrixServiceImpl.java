package com.certification.backend.service.impl;

import com.certification.backend.dto.request.ObjectiveRequirementMatrixBatchRequest;
import com.certification.backend.dto.request.ObjectiveRequirementMatrixRequest;
import com.certification.backend.dto.response.ObjectiveRequirementMatrixResponse;
import com.certification.backend.entity.EducationalObjective;
import com.certification.backend.entity.GraduationRequirement;
import com.certification.backend.entity.ObjectiveRequirementMatrix;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.EducationalObjectiveRepository;
import com.certification.backend.repository.GraduationRequirementRepository;
import com.certification.backend.repository.ObjectiveRequirementMatrixRepository;
import com.certification.backend.service.ObjectiveRequirementMatrixService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 培养目标-毕业要求支撑矩阵业务实现
 */
@Service
@Transactional
public class ObjectiveRequirementMatrixServiceImpl implements ObjectiveRequirementMatrixService {

    private final ObjectiveRequirementMatrixRepository matrixRepository;
    private final EducationalObjectiveRepository educationalObjectiveRepository;
    private final GraduationRequirementRepository graduationRequirementRepository;

    public ObjectiveRequirementMatrixServiceImpl(ObjectiveRequirementMatrixRepository matrixRepository,
                                                   EducationalObjectiveRepository educationalObjectiveRepository,
                                                   GraduationRequirementRepository graduationRequirementRepository) {
        this.matrixRepository = matrixRepository;
        this.educationalObjectiveRepository = educationalObjectiveRepository;
        this.graduationRequirementRepository = graduationRequirementRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectiveRequirementMatrixResponse> listByObjectiveId(Long objectiveId) {
        if (!educationalObjectiveRepository.existsById(objectiveId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "培养目标不存在");
        }
        return matrixRepository.findByObjectiveId(objectiveId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectiveRequirementMatrixResponse> listByRequirementId(Long requirementId) {
        if (!graduationRequirementRepository.existsById(requirementId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "毕业要求不存在");
        }
        return matrixRepository.findByRequirementId(requirementId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ObjectiveRequirementMatrixResponse add(ObjectiveRequirementMatrixRequest request) {
        // 验证关联实体存在
        if (!educationalObjectiveRepository.existsById(request.getObjectiveId())) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "培养目标不存在");
        }
        if (!graduationRequirementRepository.existsById(request.getRequirementId())) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "毕业要求不存在");
        }

        ObjectiveRequirementMatrix matrix = new ObjectiveRequirementMatrix();
        matrix.setObjectiveId(request.getObjectiveId());
        matrix.setRequirementId(request.getRequirementId());
        matrix.setSupportLevel(request.getSupportLevel());

        ObjectiveRequirementMatrix saved = matrixRepository.save(matrix);
        return toResponse(saved);
    }

    @Override
    public ObjectiveRequirementMatrixResponse update(ObjectiveRequirementMatrixRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "支撑关系ID不能为空");
        }

        ObjectiveRequirementMatrix matrix = matrixRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "支撑关系不存在"));

        if (request.getSupportLevel() != null) {
            matrix.setSupportLevel(request.getSupportLevel());
        }

        ObjectiveRequirementMatrix saved = matrixRepository.save(matrix);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!matrixRepository.existsById(id)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "支撑关系不存在");
        }
        matrixRepository.deleteById(id);
    }

    @Override
    @Transactional
    public List<ObjectiveRequirementMatrixResponse> batchSave(ObjectiveRequirementMatrixBatchRequest request) {
        Long objectiveId = request.getObjectiveId();

        // 验证培养目标存在
        if (!educationalObjectiveRepository.existsById(objectiveId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "培养目标不存在");
        }

        // 先删后插
        matrixRepository.deleteByObjectiveId(objectiveId);

        List<ObjectiveRequirementMatrix> toSave = new ArrayList<>();
        if (request.getItems() != null) {
            for (ObjectiveRequirementMatrixBatchRequest.MatrixItem item : request.getItems()) {
                // 验证毕业要求存在
                if (!graduationRequirementRepository.existsById(item.getRequirementId())) {
                    throw new BusinessException(ResultCodeEnum.NOT_FOUND, "毕业要求不存在: " + item.getRequirementId());
                }
                ObjectiveRequirementMatrix matrix = new ObjectiveRequirementMatrix();
                matrix.setObjectiveId(objectiveId);
                matrix.setRequirementId(item.getRequirementId());
                matrix.setSupportLevel(item.getSupportLevel());
                toSave.add(matrix);
            }
        }

        List<ObjectiveRequirementMatrix> saved = matrixRepository.saveAll(toSave);
        return saved.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ObjectiveRequirementMatrixResponse toResponse(ObjectiveRequirementMatrix matrix) {
        ObjectiveRequirementMatrixResponse resp = new ObjectiveRequirementMatrixResponse();
        resp.setId(matrix.getId());
        resp.setObjectiveId(matrix.getObjectiveId());
        resp.setRequirementId(matrix.getRequirementId());
        resp.setSupportLevel(matrix.getSupportLevel());
        return resp;
    }
}