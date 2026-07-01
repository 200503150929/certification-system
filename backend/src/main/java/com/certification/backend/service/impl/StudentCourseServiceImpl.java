package com.certification.backend.service.impl;

import com.certification.backend.dto.response.ImportResultResponse;
import com.certification.backend.dto.response.StudentCourseResponse;
import com.certification.backend.dto.response.StudentInfoResponse;
import com.certification.backend.entity.*;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.*;
import com.certification.backend.service.StudentCourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
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
    private final StudentGradeRepository studentGradeRepository;  // ⚠️ 新增注入

    public StudentCourseServiceImpl(StudentCourseRepository studentCourseRepository,
                                    CourseOfferingRepository courseOfferingRepository,
                                    CourseRepository courseRepository,
                                    UserRepository userRepository,
                                    StudentGradeRepository studentGradeRepository) {  // ⚠️ 新增参数
        this.studentCourseRepository = studentCourseRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.studentGradeRepository = studentGradeRepository;
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
        for (StudentCourse sc : studentCourses) {
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

    // ==================== 新增方法 ====================

    @Override
    @Transactional(readOnly = true)
    public List<StudentInfoResponse> getStudentsByOffering(Long offeringId, String teacherUsername) {
        // 1. 验证教师权限
        User teacher = getTeacherByUsername(teacherUsername);
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "开课记录不存在"));

        if (!offering.getTeacherId().equals(teacher.getId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无权查看该开课记录");
        }

        // 2. 查询选课记录
        List<StudentCourse> scList = studentCourseRepository.findByOfferingId(offeringId);
        if (scList.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 批量查询学生信息
        List<Long> studentIds = scList.stream()
                .map(StudentCourse::getStudentId)
                .collect(Collectors.toList());
        List<User> students = userRepository.findAllById(studentIds);

        // 4. 查询成绩（用于显示最终成绩）
        Map<Long, BigDecimal> gradeMap = new HashMap<>();
        for (StudentCourse sc : scList) {
            studentGradeRepository.findByOfferingIdAndStudentId(offeringId, sc.getStudentId())
                    .ifPresent(g -> gradeMap.put(sc.getStudentId(), g.getTotalScore()));
        }

        // 5. 组装返回
        Map<Long, User> studentMap = students.stream()
                .collect(Collectors.toMap(User::getId, s -> s));

        return scList.stream()
                .map(sc -> {
                    User student = studentMap.get(sc.getStudentId());
                    if (student == null) return null;
                    StudentInfoResponse resp = new StudentInfoResponse();
                    resp.setStudentId(student.getId());
                    resp.setStudentNo(student.getUsername());
                    resp.setStudentName(student.getName());
                    resp.setTotalScore(gradeMap.get(sc.getStudentId()));
                    return resp;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ImportResultResponse importStudents(Long offeringId, List<String> studentNos, String teacherUsername) {
        // 1. 验证教师权限
        User teacher = getTeacherByUsername(teacherUsername);
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "开课记录不存在"));

        if (!offering.getTeacherId().equals(teacher.getId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无权操作该开课记录");
        }

        // 2. 去重并过滤空值
        List<String> distinctStudentNos = studentNos.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        if (distinctStudentNos.isEmpty()) {
            ImportResultResponse emptyResult = new ImportResultResponse();
            emptyResult.setTotalCount(0);
            emptyResult.setSuccessCount(0);
            return emptyResult;
        }

        // 3. 查询学生（角色为 student）
        List<User> students = userRepository.findByUsernameInAndRole(distinctStudentNos, "student");

        // 4. 记录未找到的学生
        List<String> foundUsernames = students.stream()
                .map(User::getUsername)
                .collect(Collectors.toList());
        List<String> notFound = distinctStudentNos.stream()
                .filter(s -> !foundUsernames.contains(s))
                .collect(Collectors.toList());

        // 5. 批量导入
        int successCount = 0;
        List<String> alreadyExist = new ArrayList<>();

        for (User student : students) {
            // 检查是否已存在
            if (studentCourseRepository.existsByOfferingIdAndStudentId(offeringId, student.getId())) {
                alreadyExist.add(student.getUsername());
                continue;
            }

            StudentCourse sc = new StudentCourse();
            sc.setOfferingId(offeringId);
            sc.setStudentId(student.getId());
            studentCourseRepository.save(sc);
            successCount++;
        }

        // 6. 组装结果
        ImportResultResponse result = new ImportResultResponse();
        result.setTotalCount(distinctStudentNos.size());
        result.setSuccessCount(successCount);

        List<String> failedItems = new ArrayList<>();
        if (!alreadyExist.isEmpty()) {
            failedItems.add("已存在: " + String.join(", ", alreadyExist));
        }
        if (!notFound.isEmpty()) {
            failedItems.add("未找到: " + String.join(", ", notFound));
        }
        result.setFailedItems(failedItems);

        return result;
    }

    @Override
    @Transactional
    public void removeStudent(Long offeringId, Long studentId, String teacherUsername) {
        // 1. 验证教师权限
        User teacher = getTeacherByUsername(teacherUsername);
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "开课记录不存在"));

        if (!offering.getTeacherId().equals(teacher.getId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无权操作该开课记录");
        }

        // 2. 检查是否有成绩记录
        if (studentGradeRepository.existsByOfferingIdAndStudentId(offeringId, studentId)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST,
                    "该学生已有成绩记录，无法移除，请先删除成绩");
        }

        // 3. 删除选课记录
        StudentCourse sc = studentCourseRepository.findByOfferingIdAndStudentId(offeringId, studentId)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "学生不在该课程中"));
        studentCourseRepository.delete(sc);
    }

    @Override
    @Transactional
    public ImportResultResponse batchRemoveStudents(Long offeringId, List<Long> studentIds, String teacherUsername) {
        // 1. 验证教师权限
        User teacher = getTeacherByUsername(teacherUsername);
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "开课记录不存在"));

        if (!offering.getTeacherId().equals(teacher.getId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无权操作该开课记录");
        }

        // 2. 去重
        List<Long> distinctStudentIds = studentIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (distinctStudentIds.isEmpty()) {
            ImportResultResponse emptyResult = new ImportResultResponse();
            emptyResult.setTotalCount(0);
            emptyResult.setSuccessCount(0);
            return emptyResult;
        }

        int successCount = 0;
        List<String> failedItems = new ArrayList<>();

        for (Long studentId : distinctStudentIds) {
            try {
                // 检查是否有成绩记录
                if (studentGradeRepository.existsByOfferingIdAndStudentId(offeringId, studentId)) {
                    failedItems.add("学生ID " + studentId + " 已有成绩记录");
                    continue;
                }

                // 查找并删除选课记录
                StudentCourse sc = studentCourseRepository.findByOfferingIdAndStudentId(offeringId, studentId)
                        .orElse(null);
                if (sc == null) {
                    failedItems.add("学生ID " + studentId + " 不在该课程中");
                    continue;
                }

                studentCourseRepository.delete(sc);
                successCount++;
            } catch (Exception e) {
                failedItems.add("学生ID " + studentId + ": " + e.getMessage());
            }
        }

        ImportResultResponse result = new ImportResultResponse();
        result.setTotalCount(distinctStudentIds.size());
        result.setSuccessCount(successCount);
        result.setFailedItems(failedItems);
        return result;
    }

    // ==================== 私有辅助方法 ====================

    private User getTeacherByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.UNAUTHORIZED, "用户不存在"));
    }
}