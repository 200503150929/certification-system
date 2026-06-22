package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 培养目标-毕业要求支撑矩阵请求参数
 */

@Setter
@Getter
public class ObjectiveRequirementMatrixRequest {

    private Long id;

    @NotNull(message = "培养目标ID不能为空")
    private Long objectiveId;

    @NotNull(message = "毕业要求ID不能为空")
    private Long requirementId;

    @NotBlank(message = "支撑强度不能为空")
    private String supportLevel; // 强/弱

}