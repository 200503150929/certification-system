package com.certification.backend.service.impl;

import com.certification.backend.dto.request.CourseRequest;
import com.certification.backend.dto.request.CourseRequirementMatrixBatchRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.CourseDetailResponse;
import com.certification.backend.dto.response.CourseResponse;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.entity.Course;
import com.certification.backend.entity.CourseRequirementMatrix;
import com.certification.backend.entity.GraduationRequirement;
import com.certification.backend.entity.IndicatorPoint;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.CourseRepository;
import com.certification.backend.repository.CourseRequirementMatrixRepository;
import com.certification.backend.repository.GraduationRequirementRepository;
import com.certification.backend.repository.IndicatorPointRepository;
import com.certification.backend.repository.ProgramRepository;
import com.certification.backend.service.CourseService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程体系管理业务实现
 */
@Service
@Transactional
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private final CourseRepository courseRepository;
    private final CourseRequirementMatrixRepository matrixRepository;
    private final ProgramRepository programRepository;
    private final IndicatorPointRepository indicatorPointRepository;
    private final GraduationRequirementRepository graduationRequirementRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CourseServiceImpl(CourseRepository courseRepository,
                             CourseRequirementMatrixRepository matrixRepository,
                             ProgramRepository programRepository,
                             IndicatorPointRepository indicatorPointRepository,
                             GraduationRequirementRepository graduationRequirementRepository) {
        this.courseRepository = courseRepository;
        this.matrixRepository = matrixRepository;
        this.programRepository = programRepository;
        this.indicatorPointRepository = indicatorPointRepository;
        this.graduationRequirementRepository = graduationRequirementRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CourseResponse> listCourses(String keyword, Long programId, PageQuery pageQuery) {
        // 构建动态查询条件
        Specification<Course> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                Predicate codeLike = cb.like(root.get("code"), "%" + keyword + "%");
                Predicate nameLike = cb.like(root.get("name"), "%" + keyword + "%");
                predicates.add(cb.or(codeLike, nameLike));
            }

            if (programId != null) {
                predicates.add(cb.equal(root.get("programId"), programId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(
                pageQuery.getPageNum() - 1,
                pageQuery.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Course> page = courseRepository.findAll(spec, pageRequest);

        List<CourseResponse> list = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        PageResult<CourseResponse> result = new PageResult<>();
        result.setList(list);
        result.setTotal(page.getTotalElements());
        result.setPageNum(pageQuery.getPageNum());
        result.setPageSize(pageQuery.getPageSize());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "课程不存在"));

        List<CourseRequirementMatrix> matrices = matrixRepository.findByCourseId(id);

        CourseDetailResponse detail = new CourseDetailResponse();
        copyToResponse(course, detail);

        // 填充专业名称
        programRepository.findById(course.getProgramId()).ifPresent(p ->
                detail.setProgramName(p.getMajorName()));

        List<CourseDetailResponse.MatrixItemResponse> items = matrices.stream()
                .map(m -> {
                    CourseDetailResponse.MatrixItemResponse item = new CourseDetailResponse.MatrixItemResponse();
                    item.setId(m.getId());
                    item.setIndicatorId(m.getIndicatorId());
                    item.setSupportLevel(m.getSupportLevel());

                    // 查询指标点信息
                    indicatorPointRepository.findById(m.getIndicatorId()).ifPresent(ip -> {
                        item.setIndicatorCode(ip.getCode());
                        item.setIndicatorDescription(ip.getDescription());
                    });

                    return item;
                })
                .collect(Collectors.toList());

        detail.setMatrixItems(items);
        return detail;
    }

    @Override
    public CourseResponse addCourse(CourseRequest request) {
        // 校验课程代码唯一性
        if (courseRepository.existsByCode(request.getCode())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "课程代码已存在");
        }

        // 校验培养方案是否存在
        if (!programRepository.existsById(request.getProgramId())) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "培养方案不存在");
        }

        Course course = new Course();
        course.setCode(request.getCode());
        course.setName(request.getName());
        course.setCredits(request.getCredits());
        course.setTotalHours(request.getTotalHours());
        course.setTheoryHours(request.getTheoryHours());
        course.setLabHours(request.getLabHours());
        course.setSemester(request.getSemester());
        course.setCourseType(request.getCourseType());
        course.setIsRequired(request.getIsRequired());
        course.setProgramId(request.getProgramId());

        Course saved = courseRepository.save(course);
        return toResponse(saved);
    }

    @Override
    public CourseResponse updateCourse(CourseRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "课程ID不能为空");
        }

        Course course = courseRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "课程不存在"));

        // 校验课程代码唯一性（排除自身）
        if (StringUtils.hasText(request.getCode())) {
            if (courseRepository.existsByCodeAndIdNot(request.getCode(), request.getId())) {
                throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "课程代码已存在");
            }
            course.setCode(request.getCode());
        }

        // 校验培养方案是否存在（如果变更了培养方案）
        if (request.getProgramId() != null && !request.getProgramId().equals(course.getProgramId())) {
            if (!programRepository.existsById(request.getProgramId())) {
                throw new BusinessException(ResultCodeEnum.NOT_FOUND, "培养方案不存在");
            }
            course.setProgramId(request.getProgramId());
        }

        if (StringUtils.hasText(request.getName())) {
            course.setName(request.getName());
        }
        if (request.getCredits() != null) {
            course.setCredits(request.getCredits());
        }
        if (request.getTotalHours() != null) {
            course.setTotalHours(request.getTotalHours());
        }
        if (request.getTheoryHours() != null) {
            course.setTheoryHours(request.getTheoryHours());
        }
        if (request.getLabHours() != null) {
            course.setLabHours(request.getLabHours());
        }
        if (StringUtils.hasText(request.getSemester())) {
            course.setSemester(request.getSemester());
        }
        if (StringUtils.hasText(request.getCourseType())) {
            course.setCourseType(request.getCourseType());
        }
        if (request.getIsRequired() != null) {
            course.setIsRequired(request.getIsRequired());
        }

        Course saved = courseRepository.save(course);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "课程不存在");
        }

        // 先删除关联的支撑矩阵记录
        matrixRepository.deleteByCourseId(id);

        // 再删除课程
        courseRepository.deleteById(id);
    }

    @Override
    @Transactional
    public List<CourseDetailResponse.MatrixItemResponse> batchSaveMatrix(CourseRequirementMatrixBatchRequest request) {
        Long courseId = request.getCourseId();

        // 校验课程是否存在
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "课程不存在"));

        // 先删除该课程下所有旧矩阵
        matrixRepository.deleteByCourseId(courseId);

        List<CourseRequirementMatrixBatchRequest.MatrixItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        // 校验指标点是否属于该课程对应的培养方案
        List<Long> indicatorIds = items.stream()
                .map(CourseRequirementMatrixBatchRequest.MatrixItem::getIndicatorId)
                .collect(Collectors.toList());

        List<IndicatorPoint> indicatorPoints = indicatorPointRepository.findAllById(indicatorIds);
        if (indicatorPoints.size() != indicatorIds.size()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "部分指标点不存在");
        }

        // 校验每个指标点是否属于该课程培养方案下的毕业要求
        // 获取该培养方案下所有毕业要求的ID集合
        List<Long> requirementIds = graduationRequirementRepository.findByProgramId(course.getProgramId())
                .stream()
                .map(GraduationRequirement::getId)
                .collect(Collectors.toList());

        for (IndicatorPoint ip : indicatorPoints) {
            if (!requirementIds.contains(ip.getRequirementId())) {
                throw new BusinessException(ResultCodeEnum.BAD_REQUEST,
                        "指标点 " + ip.getCode() + " 不属于该课程所属的培养方案");
            }
        }

        // 遍历插入新记录
        List<CourseRequirementMatrix> newMatrices = items.stream()
                .map(item -> {
                    CourseRequirementMatrix matrix = new CourseRequirementMatrix();
                    matrix.setCourseId(courseId);
                    matrix.setIndicatorId(item.getIndicatorId());
                    matrix.setSupportLevel(item.getSupportLevel());
                    return matrix;
                })
                .collect(Collectors.toList());

        List<CourseRequirementMatrix> savedMatrices = matrixRepository.saveAll(newMatrices);

        // 转换为响应对象
        return savedMatrices.stream()
                .map(m -> {
                    CourseDetailResponse.MatrixItemResponse resp = new CourseDetailResponse.MatrixItemResponse();
                    resp.setId(m.getId());
                    resp.setIndicatorId(m.getIndicatorId());
                    resp.setSupportLevel(m.getSupportLevel());

                    indicatorPointRepository.findById(m.getIndicatorId()).ifPresent(ip -> {
                        resp.setIndicatorCode(ip.getCode());
                        resp.setIndicatorDescription(ip.getDescription());
                    });

                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> listForExport(Long programId) {
        log.info("===== 导出课程，接收到的 programId = {} =====", programId);

        List<Course> courses;
        if (programId != null) {
            // 使用 Repository 自带方法按培养方案过滤
            log.info("按培养方案 {} 过滤课程", programId);
            courses = courseRepository.findByProgramId(programId);
        } else {
            // 导出全部
            log.info("programId 为空，导出全部课程");
            courses = courseRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
        }

        log.info("查询到 {} 条课程", courses.size());
        courses.forEach(c -> log.info("课程: {}, programId: {}", c.getCode(), c.getProgramId()));

        return courses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== DTO 转换方法 ====================

    private CourseResponse toResponse(Course course) {
        CourseResponse resp = new CourseResponse();
        copyToResponse(course, resp);

        // 填充专业名称
        programRepository.findById(course.getProgramId()).ifPresent(p ->
                resp.setProgramName(p.getMajorName()));

        return resp;
    }

    private void copyToResponse(Course course, CourseResponse resp) {
        resp.setId(course.getId());
        resp.setCode(course.getCode());
        resp.setName(course.getName());
        resp.setCredits(course.getCredits());
        resp.setTotalHours(course.getTotalHours());
        resp.setTheoryHours(course.getTheoryHours());
        resp.setLabHours(course.getLabHours());
        resp.setSemester(course.getSemester());
        resp.setCourseType(course.getCourseType());
        resp.setIsRequired(course.getIsRequired());
        resp.setProgramId(course.getProgramId());
        resp.setCreatedAt(course.getCreatedAt() != null ? course.getCreatedAt().format(DTF) : null);
    }
}