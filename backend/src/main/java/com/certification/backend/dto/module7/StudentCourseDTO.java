package com.certification.backend.dto.module7;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class StudentCourseDTO {
    // 开课ID
    private Long offeringId;
    private String courseCode;
    private String courseName;
    private BigDecimal credits;
    private Integer totalHours;
    private String semester;
    private String academicYear;
    private String teacherName;
    private String courseType;

    private List<String> courseObjectives;
    private List<String> assessmentList;
    private List<CourseResourceDTO> resourceList;
    private BigDecimal totalScore;
}

// 内部类：课程课件资源
@Data
class CourseResourceDTO{
    private String fileName;
    private String filePath;
    private String uploadTime;
}