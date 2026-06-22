package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 专业/培养方案返回参数
 */
@Setter
@Getter
public class ProgramResponse {

    private Long id;
    private String majorName;
    private String version;
    private String status;
    private String createdAt;

}