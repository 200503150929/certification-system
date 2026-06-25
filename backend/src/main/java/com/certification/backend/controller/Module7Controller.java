package com.certification.backend.controller;

import com.certification.backend.dto.module7.StudentCourseDTO;
import com.certification.backend.dto.module7.TeacherStudentDTO;
import com.certification.backend.dto.module7.UserInfoDTO;
import com.certification.backend.dto.module7.UserUpdateDTO;
import com.certification.backend.service.Module7Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/module7")
@RequiredArgsConstructor
public class Module7Controller {

    private final Module7Service module7Service;

    // 获取个人信息
    @GetMapping("/user/info")
    public ResponseEntity<UserInfoDTO> getUserInfo(){
        Long loginId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        UserInfoDTO data = module7Service.getCurrentUserInfo(loginId);
        return ResponseEntity.ok(data);
    }

    // 修改手机邮箱
    @PutMapping("/user/update")
    public ResponseEntity<String> updateInfo(@Valid @RequestBody UserUpdateDTO dto){
        Long loginId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        module7Service.updateUserInfo(loginId, dto);
        return ResponseEntity.ok("修改成功");
    }

    // 学生：全部课程列表
    @GetMapping("/student/course/list")
    public ResponseEntity<List<StudentCourseDTO>> studentCourseList(){
        Long stuId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        List<StudentCourseDTO> list = module7Service.getStudentAllCourse(stuId);
        return ResponseEntity.ok(list);
    }

    // 学生：课程详情
    @GetMapping("/student/course/detail/{offeringId}")
    public ResponseEntity<StudentCourseDTO> courseDetail(@PathVariable Long offeringId){
        Long stuId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        StudentCourseDTO data = module7Service.getCourseDetail(stuId, offeringId);
        return ResponseEntity.ok(data);
    }

    // 教师：查看本班学生
    @GetMapping("/teacher/course/student/{offeringId}")
    public ResponseEntity<List<TeacherStudentDTO>> getStuList(@PathVariable Long offeringId){
        Long teaId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        List<TeacherStudentDTO> list = module7Service.getTeacherCourseStudent(teaId, offeringId);
        return ResponseEntity.ok(list);
    }
}