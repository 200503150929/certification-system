package com.certification.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色列表响应 VO
 */
@Setter
@Getter
@AllArgsConstructor
public class RoleListResponse {

    private Long id;
    private String roleName;
}
