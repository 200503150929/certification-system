package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

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
    private BigDecimal credits;
    /** 课程类型：专业必修、专业选修、公共基础、实践环节 */
    private String courseType;
    /** 是否必修 */
    private Boolean isRequired;
    private String createdAt;

    // ========== 新增字段 ==========
    /** 培养方案ID */
    private Long programId;
    /** 培养方案名称（可选） */
    private String programName;
}