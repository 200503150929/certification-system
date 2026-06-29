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
    /** 课程学分 */
    private java.math.BigDecimal credits;
    /** 课程类型：专业必修、专业选修、公共基础、实践环节 */
    private String courseType;
    /** 是否必修 */
    private Boolean isRequired;
    private String createdAt;
}
