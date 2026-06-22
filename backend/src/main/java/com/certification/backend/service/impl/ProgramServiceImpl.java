package com.certification.backend.service.impl;

import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.request.ProgramRequest;
import com.certification.backend.dto.response.*;
import com.certification.backend.entity.EducationalObjective;
import com.certification.backend.entity.GraduationRequirement;
import com.certification.backend.entity.IndicatorPoint;
import com.certification.backend.entity.Program;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.*;
import com.certification.backend.service.ProgramService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 专业/培养方案业务实现
 */
@Service
@Transactional
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepository programRepository;
    private final EducationalObjectiveRepository educationalObjectiveRepository;
    private final GraduationRequirementRepository graduationRequirementRepository;
    private final IndicatorPointRepository indicatorPointRepository;
    private final ObjectiveRequirementMatrixRepository objectiveRequirementMatrixRepository;

    public ProgramServiceImpl(ProgramRepository programRepository,
                               EducationalObjectiveRepository educationalObjectiveRepository,
                               GraduationRequirementRepository graduationRequirementRepository,
                               IndicatorPointRepository indicatorPointRepository,
                               ObjectiveRequirementMatrixRepository objectiveRequirementMatrixRepository) {
        this.programRepository = programRepository;
        this.educationalObjectiveRepository = educationalObjectiveRepository;
        this.graduationRequirementRepository = graduationRequirementRepository;
        this.indicatorPointRepository = indicatorPointRepository;
        this.objectiveRequirementMatrixRepository = objectiveRequirementMatrixRepository;
    }

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProgramResponse> listPrograms(String majorNameFuzzy, String status, PageQuery pageQuery) {
        // 构建分页请求
        PageRequest pageRequest = PageRequest.of(
                pageQuery.getPageNum() - 1,
                pageQuery.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // 查询所有数据，在内存中过滤（数据量通常较小）
        List<Program> allPrograms = programRepository.findAll();

        // 过滤
        List<Program> filtered = allPrograms.stream()
                .filter(p -> !StringUtils.hasText(majorNameFuzzy) ||
                        (p.getMajorName() != null && p.getMajorName().contains(majorNameFuzzy)))
                .filter(p -> !StringUtils.hasText(status) ||
                        (p.getStatus() != null && p.getStatus().equals(status)))
                .collect(Collectors.toList());

        // 手动分页
        int total = filtered.size();
        int fromIndex = (pageQuery.getPageNum() - 1) * pageQuery.getPageSize();
        int toIndex = Math.min(fromIndex + pageQuery.getPageSize(), total);

        List<ProgramResponse> list;
        if (fromIndex >= total) {
            list = Collections.emptyList();
        } else {
            list = filtered.subList(fromIndex, toIndex).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        PageResult<ProgramResponse> result = new PageResult<>();
        result.setList(list);
        result.setTotal((long) total);
        result.setPageNum(pageQuery.getPageNum());
        result.setPageSize(pageQuery.getPageSize());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramResponse> listAllPrograms() {
        return programRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramDetailResponse getProgramDetail(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "专业不存在"));

        // 获取培养目标
        List<EducationalObjectiveResponse> objectives = educationalObjectiveRepository
                .findByProgramIdOrderBySortOrderAsc(id)
                .stream()
                .map(this::toObjectiveResponse)
                .collect(Collectors.toList());

        // 获取毕业要求及指标点
        List<GraduationRequirement> requirements = graduationRequirementRepository.findByProgramId(id);
        List<GraduationRequirementDetailResponse> requirementDetails = requirements.stream()
                .map(req -> {
                    GraduationRequirementDetailResponse detail = new GraduationRequirementDetailResponse();
                    detail.setId(req.getId());
                    detail.setProgramId(req.getProgramId());
                    detail.setCode(req.getCode());
                    detail.setDescription(req.getDescription());
                    detail.setCreatedAt(req.getCreatedAt() != null ? req.getCreatedAt().format(DTF) : null);

                    List<IndicatorPointResponse> points = indicatorPointRepository
                            .findByRequirementId(req.getId())
                            .stream()
                            .map(this::toIndicatorPointResponse)
                            .collect(Collectors.toList());
                    detail.setIndicatorPoints(points);
                    return detail;
                })
                .collect(Collectors.toList());

        ProgramDetailResponse detailResponse = new ProgramDetailResponse();
        detailResponse.setProgram(this.toResponse(program));
        detailResponse.setObjectives(objectives);
        detailResponse.setRequirements(requirementDetails);
        return detailResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramResponse getProgramById(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "专业不存在"));
        return toResponse(program);
    }

    @Override
    public ProgramResponse addProgram(ProgramRequest request) {
        // 检查专业名称是否重复
        if (programRepository.findByMajorName(request.getMajorName()).isPresent()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "专业名称已存在");
        }

        Program program = new Program();
        program.setMajorName(request.getMajorName());
        program.setVersion(request.getVersion());
        program.setStatus(request.getStatus() != null ? request.getStatus() : "draft");

        Program saved = programRepository.save(program);
        return toResponse(saved);
    }

    @Override
    public ProgramResponse updateProgram(ProgramRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "专业ID不能为空");
        }

        Program program = programRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "专业不存在"));

        // 检查专业名称是否与其他记录重复
        if (StringUtils.hasText(request.getMajorName())) {
            programRepository.findByMajorName(request.getMajorName()).ifPresent(existing -> {
                if (!existing.getId().equals(request.getId())) {
                    throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "专业名称已存在");
                }
            });
            program.setMajorName(request.getMajorName());
        }

        if (StringUtils.hasText(request.getVersion())) {
            program.setVersion(request.getVersion());
        }
        if (StringUtils.hasText(request.getStatus())) {
            program.setStatus(request.getStatus());
        }

        Program saved = programRepository.save(program);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProgram(Long id) {
        if (!programRepository.existsById(id)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "专业不存在");
        }

        // 级联删除：先删除依赖数据
        // 删除支撑矩阵
        List<EducationalObjective> objectives = educationalObjectiveRepository.findByProgramIdOrderBySortOrderAsc(id);
        for (EducationalObjective obj : objectives) {
            objectiveRequirementMatrixRepository.deleteByObjectiveId(obj.getId());
        }

        // 删除培养目标
        educationalObjectiveRepository.deleteByProgramId(id);

        // 删除毕业要求及指标点
        List<GraduationRequirement> requirements = graduationRequirementRepository.findByProgramId(id);
        for (GraduationRequirement req : requirements) {
            indicatorPointRepository.deleteByRequirementId(req.getId());
        }
        graduationRequirementRepository.deleteByProgramId(id);

        // 最后删除专业
        programRepository.deleteById(id);
    }

    @Override
    public void publishProgram(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "专业不存在"));
        program.setStatus("published");
        programRepository.save(program);
    }

    @Override
    public void unpublishProgram(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "专业不存在"));
        program.setStatus("draft");
        programRepository.save(program);
    }

    // ==================== DTO 转换方法 ====================

    private ProgramResponse toResponse(Program program) {
        ProgramResponse resp = new ProgramResponse();
        resp.setId(program.getId());
        resp.setMajorName(program.getMajorName());
        resp.setVersion(program.getVersion());
        resp.setStatus(program.getStatus());
        resp.setCreatedAt(program.getCreatedAt() != null ? program.getCreatedAt().format(DTF) : null);
        return resp;
    }

    private EducationalObjectiveResponse toObjectiveResponse(EducationalObjective obj) {
        EducationalObjectiveResponse resp = new EducationalObjectiveResponse();
        resp.setId(obj.getId());
        resp.setProgramId(obj.getProgramId());
        resp.setDescription(obj.getDescription());
        resp.setSortOrder(obj.getSortOrder());
        resp.setCreatedAt(obj.getCreatedAt() != null ? obj.getCreatedAt().format(DTF) : null);
        return resp;
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