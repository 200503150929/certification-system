package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 毕业要求返回参数
 */
@Setter
@Getter
public class GraduationRequirementResponse {

    private Long id;
    private Long programId;
    private String code;
    private String description;
    private String createdAt;

}