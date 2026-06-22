package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 培养方案详情（含培养目标、毕业要求、指标点等完整层级）
 */
@Setter
@Getter
public class ProgramDetailResponse {

    private ProgramResponse program;
    private List<EducationalObjectiveResponse> objectives;
    private List<GraduationRequirementDetailResponse> requirements;

}