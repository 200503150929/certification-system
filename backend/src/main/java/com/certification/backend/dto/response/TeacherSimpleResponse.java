package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 教师简要信息（用于下拉选择）
 */
@Setter
@Getter
public class TeacherSimpleResponse {
    private Long id;
    private String name;
    private String username;
    private String college;
}