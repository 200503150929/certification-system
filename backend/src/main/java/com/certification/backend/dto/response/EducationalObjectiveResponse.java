package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 培养目标返回参数
 */

@Setter
@Getter
public class EducationalObjectiveResponse {

    private Long id;
    private Long programId;
    private String description;
    private Integer sortOrder;
    private String createdAt;

}