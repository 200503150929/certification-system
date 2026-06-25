package com.certification.backend.dto.module7;

import lombok.Data;

@Data
public class TeacherStudentDTO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String majorName;
    private String grade;
    private String className;
    private String phone;
    private String email;
}