package com.certification.backend.service.impl;

import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.request.ProgramRequest;
import com.certification.backend.dto.response.*;
import com.certification.backend.entity.*;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.enums.SupportLevelEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.*;
import com.certification.backend.service.ProgramService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final CourseRepository courseRepository;
    private final CourseRequirementMatrixRepository courseRequirementMatrixRepository;

    public ProgramServiceImpl(ProgramRepository programRepository,
                              EducationalObjectiveRepository educationalObjectiveRepository,
                              GraduationRequirementRepository graduationRequirementRepository,
                              IndicatorPointRepository indicatorPointRepository,
                              ObjectiveRequirementMatrixRepository objectiveRequirementMatrixRepository,
                              CourseRepository courseRepository,
                              CourseRequirementMatrixRepository courseRequirementMatrixRepository) {
        this.programRepository = programRepository;
        this.educationalObjectiveRepository = educationalObjectiveRepository;
        this.graduationRequirementRepository = graduationRequirementRepository;
        this.indicatorPointRepository = indicatorPointRepository;
        this.objectiveRequirementMatrixRepository = objectiveRequirementMatrixRepository;
        this.courseRepository = courseRepository;
        this.courseRequirementMatrixRepository = courseRequirementMatrixRepository;
    }

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProgramResponse> listPrograms(String majorNameFuzzy, String status, PageQuery pageQuery) {
        // 构建分页请求
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(
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
        // 删除培养目标-毕业要求支撑矩阵
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

        // 删除课程及课程-指标点矩阵
        List<Course> courses = courseRepository.findByProgramId(id);
        for (Course course : courses) {
            courseRequirementMatrixRepository.deleteByCourseId(course.getId());
        }
        courseRepository.deleteByProgramId(id);

        // 最后删除专业
        programRepository.deleteById(id);
    }

    /**
     * 发布前校验数据完整性
     * @param programId 专业ID
     * @return 错误信息列表，为空表示校验通过
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> validateProgramData(Long programId) {
        List<String> errors = new ArrayList<>();

        // ============ 1. 检查培养目标 - 至少1个 ============
        List<EducationalObjective> objectives = educationalObjectiveRepository.findByProgramIdOrderBySortOrderAsc(programId);
        if (objectives.isEmpty()) {
            errors.add("请至少配置一个培养目标");
            return errors; // 如果连目标都没有，后续校验没有意义，直接返回
        }

        // ============ 2. 检查毕业要求 - 至少1条 ============
        List<GraduationRequirement> requirements = graduationRequirementRepository.findByProgramId(programId);
        if (requirements.isEmpty()) {
            errors.add("请至少配置一条毕业要求");
            return errors;
        }

        // ============ 3. 检查指标点 - 每条毕业要求至少1个 ============
        boolean hasIndicatorError = false;
        for (GraduationRequirement req : requirements) {
            List<IndicatorPoint> indicators = indicatorPointRepository.findByRequirementId(req.getId());
            if (indicators.isEmpty() && !hasIndicatorError) {
                errors.add("毕业要求 \"" + req.getCode() + "\" 缺少指标点配置");
                hasIndicatorError = true; // 只报一次，避免重复
            }
        }

        // ============ 4. 检查课程体系 - 至少1门 ============
        long courseCount = courseRepository.countByProgramId(programId);
        if (courseCount == 0) {
            errors.add("请至少添加一门课程");
        }

        // ============ 5. 检查目标-要求矩阵 - 每个目标至少支撑1条毕业要求 ============
        boolean hasObjectiveMatrixError = false;
        for (EducationalObjective obj : objectives) {
            List<ObjectiveRequirementMatrix> matrixItems = objectiveRequirementMatrixRepository.findByObjectiveId(obj.getId());
            boolean hasValidSupport = matrixItems.stream()
                    .anyMatch(item -> item.getSupportLevel() != null &&
                            SupportLevelEnum.isValid(item.getSupportLevel()));

            if (!hasValidSupport && !hasObjectiveMatrixError) {
                String shortDesc = obj.getDescription();
                if (shortDesc.length() > 15) {
                    shortDesc = shortDesc.substring(0, 15) + "...";
                }
                errors.add("培养目标 \"" + shortDesc + "\" 未配置任何支撑关系");
                hasObjectiveMatrixError = true; // 只报一次，避免重复
            }
        }

        // ============ 6. 检查课程-要求矩阵 - 每门课程至少支撑1个指标点 ============
        List<Course> courses = courseRepository.findByProgramId(programId);
        boolean hasCourseMatrixError = false;
        for (Course course : courses) {
            List<CourseRequirementMatrix> matrixItems = courseRequirementMatrixRepository.findByCourseId(course.getId());
            boolean hasValidSupport = matrixItems.stream()
                    .anyMatch(item -> item.getSupportLevel() != null &&
                            SupportLevelEnum.isValid(item.getSupportLevel()));

            if (!hasValidSupport && !hasCourseMatrixError) {
                errors.add("课程 \"" + course.getName() + "\" 未配置任何指标点支撑关系");
                hasCourseMatrixError = true; // 只报一次，避免重复
            }
        }

        return errors;
    }

    @Override
    public void publishProgram(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "专业不存在"));

        // 检查状态
        if (!"draft".equals(program.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "只有草稿状态的方案可以发布");
        }

        // 数据完整性校验（双重保障，Controller 层已经校验过）
        List<String> errors = validateProgramData(id);
        if (!errors.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST,
                    "数据不完整，无法发布：" + String.join("；", errors));
        }

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