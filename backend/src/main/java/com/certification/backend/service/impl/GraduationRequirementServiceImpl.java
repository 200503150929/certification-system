package com.certification.backend.service.impl;

import com.certification.backend.dto.request.GraduationRequirementRequest;
import com.certification.backend.dto.response.GraduationRequirementDetailResponse;
import com.certification.backend.dto.response.GraduationRequirementResponse;
import com.certification.backend.dto.response.IndicatorPointResponse;
import com.certification.backend.entity.GraduationRequirement;
import com.certification.backend.entity.IndicatorPoint;
import com.certification.backend.entity.Program;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.GraduationRequirementRepository;
import com.certification.backend.repository.IndicatorPointRepository;
import com.certification.backend.repository.ProgramRepository;
import com.certification.backend.service.GraduationRequirementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 毕业要求业务实现
 */
@Service
@Transactional
public class GraduationRequirementServiceImpl implements GraduationRequirementService {

    private final GraduationRequirementRepository graduationRequirementRepository;
    private final IndicatorPointRepository indicatorPointRepository;
    private final ProgramRepository programRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public GraduationRequirementServiceImpl(GraduationRequirementRepository graduationRequirementRepository,
                                             IndicatorPointRepository indicatorPointRepository,
                                             ProgramRepository programRepository) {
        this.graduationRequirementRepository = graduationRequirementRepository;
        this.indicatorPointRepository = indicatorPointRepository;
        this.programRepository = programRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GraduationRequirementResponse> listByProgramId(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "培养方案不存在");
        }
        return graduationRequirementRepository.findByProgramId(programId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GraduationRequirementDetailResponse> listDetailByProgramId(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "培养方案不存在");
        }

        List<GraduationRequirement> requirements = graduationRequirementRepository.findByProgramId(programId);
        return requirements.stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GraduationRequirementResponse getById(Long id) {
        GraduationRequirement req = graduationRequirementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "毕业要求不存在"));
        return toResponse(req);
    }

    @Override
    public GraduationRequirementResponse add(GraduationRequirementRequest request) {
        if (!programRepository.existsById(request.getProgramId())) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "培养方案不存在");
        }

        GraduationRequirement req = new GraduationRequirement();
        req.setProgramId(request.getProgramId());
        req.setCode(request.getCode());
        req.setDescription(request.getDescription());

        GraduationRequirement saved = graduationRequirementRepository.save(req);
        return toResponse(saved);
    }

    @Override
    public GraduationRequirementResponse update(GraduationRequirementRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "毕业要求ID不能为空");
        }

        GraduationRequirement req = graduationRequirementRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "毕业要求不存在"));

        if (request.getCode() != null) {
            req.setCode(request.getCode());
        }
        if (request.getDescription() != null) {
            req.setDescription(request.getDescription());
        }

        GraduationRequirement saved = graduationRequirementRepository.save(req);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!graduationRequirementRepository.existsById(id)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "毕业要求不存在");
        }
        // 级联删除指标点
        indicatorPointRepository.deleteByRequirementId(id);
        graduationRequirementRepository.deleteById(id);
    }

    private GraduationRequirementResponse toResponse(GraduationRequirement req) {
        GraduationRequirementResponse resp = new GraduationRequirementResponse();
        resp.setId(req.getId());
        resp.setProgramId(req.getProgramId());
        resp.setCode(req.getCode());
        resp.setDescription(req.getDescription());
        resp.setCreatedAt(req.getCreatedAt() != null ? req.getCreatedAt().format(DTF) : null);
        return resp;
    }

    private GraduationRequirementDetailResponse toDetailResponse(GraduationRequirement req) {
        GraduationRequirementDetailResponse detail = new GraduationRequirementDetailResponse();
        detail.setId(req.getId());
        detail.setProgramId(req.getProgramId());
        detail.setCode(req.getCode());
        detail.setDescription(req.getDescription());
        detail.setCreatedAt(req.getCreatedAt() != null ? req.getCreatedAt().format(DTF) : null);

        List<IndicatorPointResponse> points = indicatorPointRepository.findByRequirementId(req.getId())
                .stream()
                .map(this::toIndicatorPointResponse)
                .collect(Collectors.toList());
        detail.setIndicatorPoints(points);
        return detail;
    }

    private IndicatorPointResponse toIndicatorPointResponse(IndicatorPoint point) {
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