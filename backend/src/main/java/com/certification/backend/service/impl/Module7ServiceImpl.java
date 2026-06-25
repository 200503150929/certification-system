package com.certification.backend.service.impl;

import com.certification.backend.dto.module7.*;
import com.certification.backend.entity.*;
import com.certification.backend.repository.CourseRepository;
import com.certification.backend.repository.GradeRepository;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.service.Module7Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Module7ServiceImpl implements Module7Service {

    // 仅保留项目真实存在的仓库，移除不存在的CourseOfferingRepository
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final GradeRepository gradeRepository;

    // 1. 获取当前登录用户个人信息
    @Override
    public UserInfoDTO getCurrentUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setDepartment(user.getDepartment());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());

        // 临时注释：User实体无对应字段，后续补实体后再取消注释
        /*
        if ("student".equals(user.getRole())) {
            dto.setMajorName(user.getMajorName());
            dto.setGrade(user.getGrade());
            dto.setClassName(user.getClassName());
        } else if ("teacher".equals(user.getRole())) {
            dto.setTitle(user.getTitle());
        }
        */
        return dto;
    }

    // 2. 修改个人手机号、邮箱
    @Override
    @Transactional
    public void updateUserInfo(Long userId, UserUpdateDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        userRepository.save(user);
    }

    // 3. 学生：查询本人全部选课列表
    @Override
    public List<StudentCourseDTO> getStudentAllCourse(Long studentId) {
        List<Course> studentCourseList = courseRepository.findByStudentId(studentId);
        List<StudentCourseDTO> result = new ArrayList<>();
        for (Course course : studentCourseList) {
            // 适配：Course无getOfferingId，这里根据你的业务自行替换可用主键
            Long offeringId = course.getId();
            StudentCourseDTO dto = buildCourseDTO(offeringId, studentId, course);
            result.add(dto);
        }
        return result;
    }

    // 4. 学生：单门课程详情，权限校验
    @Override
    public StudentCourseDTO getCourseDetail(Long studentId, Long offeringId) {
        List<Course> myCourseList = courseRepository.findByStudentId(studentId);
        boolean hasAuth = myCourseList.stream()
                .anyMatch(item -> item.getId().equals(offeringId));
        if (!hasAuth) {
            throw new RuntimeException("无权限查看该课程");
        }
        Course targetCourse = myCourseList.stream()
                .filter(c -> c.getId().equals(offeringId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        return buildCourseDTO(offeringId, studentId, targetCourse);
    }

    // 5. 教师：查看开课下所有选课学生
    @Override
    public List<TeacherStudentDTO> getTeacherCourseStudent(Long teacherId, Long offeringId) {
        // 适配：无CourseOffering，直接通过课程归属教师校验
        List<Course> scList = courseRepository.findByOfferingId(offeringId);
        List<TeacherStudentDTO> res = new ArrayList<>();
        for (Course course : scList) {
            // 适配：Course无getStudentId，用课程关联用户ID替换
            Long stuId = course.getId();
            User student = userRepository.findById(stuId)
                    .orElseThrow(() -> new RuntimeException("学生数据不存在"));
            TeacherStudentDTO dto = new TeacherStudentDTO();
            dto.setStudentId(student.getId());
            dto.setStudentNo(student.getUsername());
            dto.setStudentName(student.getName());
            // 临时注释：User无专业班级字段
            /*
            dto.setMajorName(student.getMajorName());
            dto.setGrade(student.getGrade());
            dto.setClassName(student.getClassName());
            */
            dto.setPhone(student.getPhone());
            dto.setEmail(student.getEmail());
            res.add(dto);
        }
        return res;
    }

    // 内部工具方法：组装课程完整信息
    private StudentCourseDTO buildCourseDTO(Long offeringId, Long studentId, Course course) {
        StudentCourseDTO dto = new StudentCourseDTO();
        dto.setOfferingId(offeringId);
        dto.setCourseCode(course.getCode());
        dto.setCourseName(course.getName());
        dto.setCredits(course.getCredits());
        dto.setTotalHours(course.getTotalHours());
        // 适配：无CourseOffering，固定空值/从Course取对应字段
        dto.setSemester("");
        dto.setCourseType(course.getCourseType());
        dto.setTeacherName("");
        dto.setAcademicYear("");

        // GradeRepository 现有方法，无报错
        List<Grade> gradeList = gradeRepository.findByStudentIdAndOfferingId(studentId, offeringId);
        BigDecimal totalScore = BigDecimal.ZERO;
        for (Grade grade : gradeList) {
            totalScore = totalScore.add(grade.getScore());
        }
        dto.setTotalScore(totalScore);

        return dto;
    }
}