package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 新增/编辑开课记录请求参数
 */
@Setter
@Getter
public class CourseOfferingRequest {

    private Long id;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotBlank(message = "学年不能为空")
    private String academicYear;

    @NotBlank(message = "学期不能为空")
    private String semester;
}
