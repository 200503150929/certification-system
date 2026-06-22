package com.certification.backend.service.impl;

import com.certification.backend.dto.request.IndicatorPointRequest;
import com.certification.backend.dto.response.IndicatorPointResponse;
import com.certification.backend.entity.GraduationRequirement;
import com.certification.backend.entity.IndicatorPoint;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.GraduationRequirementRepository;
import com.certification.backend.repository.IndicatorPointRepository;
import com.certification.backend.service.IndicatorPointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 指标点业务实现
 */
@Service
@Transactional
public class IndicatorPointServiceImpl implements IndicatorPointService {

    private final IndicatorPointRepository indicatorPointRepository;
    private final GraduationRequirementRepository graduationRequirementRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public IndicatorPointServiceImpl(IndicatorPointRepository indicatorPointRepository,
                                      GraduationRequirementRepository graduationRequirementRepository) {
        this.indicatorPointRepository = indicatorPointRepository;
        this.graduationRequirementRepository = graduationRequirementRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndicatorPointResponse> listByRequirementId(Long requirementId) {
        if (!graduationRequirementRepository.existsById(requirementId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "毕业要求不存在");
        }
        return indicatorPointRepository.findByRequirementId(requirementId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public IndicatorPointResponse getById(Long id) {
        IndicatorPoint point = indicatorPointRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "指标点不存在"));
        return toResponse(point);
    }

    @Override
    public IndicatorPointResponse add(IndicatorPointRequest request) {
        if (!graduationRequirementRepository.existsById(request.getRequirementId())) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "毕业要求不存在");
        }

        IndicatorPoint point = new IndicatorPoint();
        point.setRequirementId(request.getRequirementId());
        point.setCode(request.getCode());
        point.setDescription(request.getDescription());
        point.setWeight(request.getWeight());
        point.setPassScore(request.getPassScore());

        IndicatorPoint saved = indicatorPointRepository.save(point);
        return toResponse(saved);
    }

    @Override
    public IndicatorPointResponse update(IndicatorPointRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "指标点ID不能为空");
        }

        IndicatorPoint point = indicatorPointRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "指标点不存在"));

        if (request.getCode() != null) {
            point.setCode(request.getCode());
        }
        if (request.getDescription() != null) {
            point.setDescription(request.getDescription());
        }
        if (request.getWeight() != null) {
            point.setWeight(request.getWeight());
        }
        if (request.getPassScore() != null) {
            point.setPassScore(request.getPassScore());
        }

        IndicatorPoint saved = indicatorPointRepository.save(point);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!indicatorPointRepository.existsById(id)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "指标点不存在");
        }
        indicatorPointRepository.deleteById(id);
    }

    private IndicatorPointResponse toResponse(IndicatorPoint point) {
        IndicatorPointResponse resp = new IndicatorPointResponse();
        resp.setId(point.getId());
        resp.setRequirementId(point.getRequirementId());
        resp.setCode(point.getCode());
        resp.setDescription(point.getDescription());
        resp.setWeight(point.getWeight());
        resp.setPassScore(point.getPassScore());
        resp.setCreatedAt(point.getCreatedAt() != null ? point.getCreatedAt().format(DTF) : null);
        return resp;
    }
}