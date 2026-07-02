package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 权限标识符响应 VO
 */
@Setter
@Getter
public class PermissionResponse {

    private Long id;
    /** 权限标识符，如 user:list */
    private String permissionCode;
    /** 权限名称，用于前端显示 */
    private String permissionName;
    /** 所属模块 */
    private String module;
    /** 排序号 */
    private Integer sortOrder;

    /** 当前角色是否拥有此权限（用于角色编辑界面） */
    private boolean assigned;
}
