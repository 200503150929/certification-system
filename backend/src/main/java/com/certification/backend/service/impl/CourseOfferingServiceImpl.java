package com.certification.backend.service.impl;

import com.certification.backend.dto.request.CourseOfferingRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.CourseOfferingResponse;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.entity.Course;
import com.certification.backend.entity.CourseOffering;
import com.certification.backend.entity.User;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.CourseOfferingRepository;
import com.certification.backend.repository.CourseRepository;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.service.CourseOfferingService;
import jakarta.persistence.criteria.Predicate;
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
 * 开课信息管理业务实现
 */
@Service
@Transactional
public class CourseOfferingServiceImpl implements CourseOfferingService {

    private final CourseOfferingRepository offeringRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CourseOfferingServiceImpl(CourseOfferingRepository offeringRepository,
                                     CourseRepository courseRepository,
                                     UserRepository userRepository) {
        this.offeringRepository = offeringRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CourseOfferingResponse> listOfferings(String username, String keyword, PageQuery pageQuery) {
        User teacher = getTeacherByUsername(username);

        Specification<CourseOffering> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("teacherId"), teacher.getId()));

            if (StringUtils.hasText(keyword)) {
                List<Long> courseIds = courseRepository.findAll().stream()
                        .filter(c -> (c.getCode() != null && c.getCode().contains(keyword))
                                || (c.getName() != null && c.getName().contains(keyword)))
                        .map(Course::getId)
                        .collect(Collectors.toList());

                if (courseIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("courseId").in(courseIds));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(
                pageQuery.getPageNum() - 1,
                pageQuery.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<CourseOffering> page = offeringRepository.findAll(spec, pageRequest);

        List<CourseOfferingResponse> list = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        PageResult<CourseOfferingResponse> result = new PageResult<>();
        result.setList(list);
        result.setTotal(page.getTotalElements());
        result.setPageNum(pageQuery.getPageNum());
        result.setPageSize(pageQuery.getPageSize());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseOfferingResponse getOfferingDetail(Long id, String username) {
        User teacher = getTeacherByUsername(username);

        CourseOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "开课记录不存在"));

        if (!offering.getTeacherId().equals(teacher.getId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无权访问该开课记录");
        }

        return toResponse(offering);
    }

    @Override
    public CourseOfferingResponse addOffering(CourseOfferingRequest request, String username) {
        User teacher = getTeacherByUsername(username);

        courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "课程不存在"));

        if (offeringRepository.existsByCourseIdAndTeacherIdAndAcademicYearAndSemester(
                request.getCourseId(), teacher.getId(), request.getAcademicYear(), request.getSemester())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "该课程在当前学年学期已存在开课记录");
        }

        CourseOffering offering = new CourseOffering();
        offering.setCourseId(request.getCourseId());
        offering.setTeacherId(teacher.getId());
        offering.setAcademicYear(request.getAcademicYear());
        offering.setSemester(request.getSemester());

        CourseOffering saved = offeringRepository.save(offering);
        return toResponse(saved);
    }

    @Override
    public CourseOfferingResponse updateOffering(CourseOfferingRequest request, String username) {
        if (request.getId() == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "开课记录ID不能为空");
        }

        User teacher = getTeacherByUsername(username);

        CourseOffering offering = offeringRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "开课记录不存在"));

        if (!offering.getTeacherId().equals(teacher.getId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无权修改该开课记录");
        }

        if (request.getCourseId() != null && !request.getCourseId().equals(offering.getCourseId())) {
            courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "课程不存在"));
            offering.setCourseId(request.getCourseId());
        }

        if (StringUtils.hasText(request.getAcademicYear())) {
            offering.setAcademicYear(request.getAcademicYear());
        }
        if (StringUtils.hasText(request.getSemester())) {
            offering.setSemester(request.getSemester());
        }

        if (offeringRepository.existsByCourseIdAndTeacherIdAndAcademicYearAndSemesterAndIdNot(
                offering.getCourseId(), teacher.getId(), offering.getAcademicYear(), offering.getSemester(), offering.getId())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "该课程在当前学年学期已存在开课记录");
        }

        CourseOffering saved = offeringRepository.save(offering);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteOffering(Long id, String username) {
        User teacher = getTeacherByUsername(username);

        CourseOffering offering = offeringRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "开课记录不存在"));

        if (!offering.getTeacherId().equals(teacher.getId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无权删除该开课记录");
        }

        offeringRepository.deleteById(id);
    }

    // ==================== 私有方法 ====================

    private User getTeacherByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.UNAUTHORIZED, "用户不存在"));
    }

    private CourseOfferingResponse toResponse(CourseOffering offering) {
        CourseOfferingResponse resp = new CourseOfferingResponse();
        resp.setId(offering.getId());
        resp.setCourseId(offering.getCourseId());
        resp.setTeacherId(offering.getTeacherId());
        resp.setAcademicYear(offering.getAcademicYear());
        resp.setSemester(offering.getSemester());
        resp.setCreatedAt(offering.getCreatedAt() != null ? offering.getCreatedAt().format(DTF) : null);

        courseRepository.findById(offering.getCourseId()).ifPresent(c -> {
            resp.setCourseCode(c.getCode());
            resp.setCourseName(c.getName());
        });

        userRepository.findById(offering.getTeacherId()).ifPresent(u ->
                resp.setTeacherName(u.getName()));

        return resp;
    }
}
