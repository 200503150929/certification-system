package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 课程资源返回参数
 */
@Setter
@Getter
public class CourseResourceResponse {

    private Long id;
    private Long offeringId;
    private String fileName;
    private String filePath;
    private String resourceType;
    private String uploadTime;
}
