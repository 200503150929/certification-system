package com.certification.backend.service.impl;

import com.certification.backend.dto.response.StudentCourseResponse;
import com.certification.backend.entity.*;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.*;
import com.certification.backend.service.StudentCourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学生课程服务实现类
 */
@Service
@Transactional
public class StudentCourseServiceImpl implements StudentCourseService {

    private final StudentCourseRepository studentCourseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public StudentCourseServiceImpl(StudentCourseRepository studentCourseRepository,
                                    CourseOfferingRepository courseOfferingRepository,
                                    CourseRepository courseRepository,
                                    UserRepository userRepository) {
        this.studentCourseRepository = studentCourseRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentCourseResponse> getStudentCourses(String username) {
        // 1. 查找学生用户
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.USER_NOT_FOUND));

        // 2. 查询该学生的所有选课记录
        List<StudentCourse> studentCourses = studentCourseRepository.findByStudentId(student.getId());

        if (studentCourses.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 批量查询开课记录
        List<Long> offeringIds = studentCourses.stream()
                .map(StudentCourse::getOfferingId)
                .collect(Collectors.toList());
        List<CourseOffering> offerings = courseOfferingRepository.findAllById(offeringIds);

        // 4. 批量查询课程信息
        Map<Long, Course> courseMap = offerings.stream()
                .map(o -> courseRepository.findById(o.getCourseId()).orElse(null))
                .filter(c -> c != null)
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));

        // 5. 批量查询教师信息
        Map<Long, User> teacherMap = offerings.stream()
                .map(o -> userRepository.findById(o.getTeacherId()).orElse(null))
                .filter(t -> t != null)
                .collect(Collectors.toMap(User::getId, t -> t, (a, b) -> a));

        // 6. 组装返回结果
        List<StudentCourseResponse> result = new ArrayList<>();
        for (int i = 0; i < studentCourses.size(); i++) {
            StudentCourse sc = studentCourses.get(i);
            CourseOffering offering = offerings.stream()
                    .filter(o -> o.getId().equals(sc.getOfferingId()))
                    .findFirst().orElse(null);
            if (offering == null) continue;

            Course course = courseMap.get(offering.getCourseId());
            if (course == null) continue;

            User teacher = teacherMap.get(offering.getTeacherId());

            StudentCourseResponse resp = new StudentCourseResponse();
            resp.setOfferingId(offering.getId());
            resp.setCourseId(course.getId());
            resp.setCourseCode(course.getCode());
            resp.setCourseName(course.getName());
            resp.setCredits(course.getCredits());
            resp.setTeacherName(teacher != null ? teacher.getName() : null);
            resp.setAcademicYear(offering.getAcademicYear());
            resp.setSemester(offering.getSemester());
            resp.setCourseType(course.getCourseType());
            resp.setIsRequired(course.getIsRequired());
            result.add(resp);
        }

        return result;
    }
}
