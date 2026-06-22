package com.certification.backend.service.impl;

import com.certification.backend.dto.request.EducationalObjectiveRequest;
import com.certification.backend.dto.response.EducationalObjectiveResponse;
import com.certification.backend.entity.EducationalObjective;
import com.certification.backend.entity.Program;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.EducationalObjectiveRepository;
import com.certification.backend.repository.ProgramRepository;
import com.certification.backend.service.EducationalObjectiveService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 培养目标业务实现
 */
@Service
@Transactional
public class EducationalObjectiveServiceImpl implements EducationalObjectiveService {

    private final EducationalObjectiveRepository educationalObjectiveRepository;
    private final ProgramRepository programRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EducationalObjectiveServiceImpl(EducationalObjectiveRepository educationalObjectiveRepository,
                                            ProgramRepository programRepository) {
        this.educationalObjectiveRepository = educationalObjectiveRepository;
        this.programRepository = programRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EducationalObjectiveResponse> listByProgramId(Long programId) {
        // 验证培养方案存在
        if (!programRepository.existsById(programId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "培养方案不存在");
        }
        return educationalObjectiveRepository.findByProgramIdOrderBySortOrderAsc(programId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EducationalObjectiveResponse getById(Long id) {
        EducationalObjective obj = educationalObjectiveRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "培养目标不存在"));
        return toResponse(obj);
    }

    @Override
    public EducationalObjectiveResponse add(EducationalObjectiveRequest request) {
        // 验证培养方案存在
        if (!programRepository.existsById(request.getProgramId())) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "培养方案不存在");
        }

        EducationalObjective obj = new EducationalObjective();
        obj.setProgramId(request.getProgramId());
        obj.setDescription(request.getDescription());
        obj.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        EducationalObjective saved = educationalObjectiveRepository.save(obj);
        return toResponse(saved);
    }

    @Override
    public EducationalObjectiveResponse update(EducationalObjectiveRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "培养目标ID不能为空");
        }

        EducationalObjective obj = educationalObjectiveRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "培养目标不存在"));

        if (request.getDescription() != null) {
            obj.setDescription(request.getDescription());
        }
        if (request.getSortOrder() != null) {
            obj.setSortOrder(request.getSortOrder());
        }

        EducationalObjective saved = educationalObjectiveRepository.save(obj);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!educationalObjectiveRepository.existsById(id)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "培养目标不存在");
        }
        educationalObjectiveRepository.deleteById(id);
    }

    private EducationalObjectiveResponse toResponse(EducationalObjective obj) {
        EducationalObjectiveResponse resp = new EducationalObjectiveResponse();
        resp.setId(obj.getId());
        resp.setProgramId(obj.getProgramId());
        resp.setDescription(obj.getDescription());
        resp.setSortOrder(obj.getSortOrder());
        resp.setCreatedAt(obj.getCreatedAt() != null ? obj.getCreatedAt().format(DTF) : null);
        return resp;
    }
}