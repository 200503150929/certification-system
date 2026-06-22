package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 培养目标-毕业要求支撑矩阵返回参数
 */
@Setter
@Getter
public class ObjectiveRequirementMatrixResponse {

    private Long id;
    private Long objectiveId;
    private Long requirementId;
    private String supportLevel;

}