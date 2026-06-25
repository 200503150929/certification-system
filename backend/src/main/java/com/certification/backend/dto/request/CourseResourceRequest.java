package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 新增课程资源请求参数
 */
@Setter
@Getter
public class CourseResourceRequest {

    @NotNull(message = "开课ID不能为空")
    private Long offeringId;

    @NotBlank(message = "资源名称不能为空")
    private String resourceName;

    @NotBlank(message = "资源地址不能为空")
    private String resourceUrl;

    @NotBlank(message = "资源类型不能为空")
    private String resourceType;
}
