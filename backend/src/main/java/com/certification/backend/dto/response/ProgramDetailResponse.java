package com.certification.backend.dto.response;

import java.util.List;

/**
 * 培养方案详情（含培养目标、毕业要求、指标点等完整层级）
 */
public class ProgramDetailResponse {

    private ProgramResponse program;
    private List<EducationalObjectiveResponse> objectives;
    private List<GraduationRequirementDetailResponse> requirements;

    public ProgramResponse getProgram() {
        return program;
    }

    public void setProgram(ProgramResponse program) {
        this.program = program;
    }

    public List<EducationalObjectiveResponse> getObjectives() {
        return objectives;
    }

    public void setObjectives(List<EducationalObjectiveResponse> objectives) {
        this.objectives = objectives;
    }

    public List<GraduationRequirementDetailResponse> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<GraduationRequirementDetailResponse> requirements) {
        this.requirements = requirements;
    }
}