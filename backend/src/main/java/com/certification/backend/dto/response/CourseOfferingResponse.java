package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 开课记录列表返回参数
 */
@Setter
@Getter
public class CourseOfferingResponse {

    private Long id;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Long teacherId;
    private String teacherName;
    private String academicYear;
    private String semester;
    private String createdAt;
}
