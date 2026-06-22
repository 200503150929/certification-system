package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 培养目标新增/编辑请求参数
 */
@Setter
@Getter
public class EducationalObjectiveRequest {

    private Long id;

    @NotNull(message = "所属培养方案不能为空")
    private Long programId;

    @NotNull(message = "培养目标描述不能为空")
    private String description;

    private Integer sortOrder;

}