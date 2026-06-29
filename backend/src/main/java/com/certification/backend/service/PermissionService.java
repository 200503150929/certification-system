package com.certification.backend.service;

import com.certification.backend.dto.response.PermissionResponse;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 获取当前登录用户的权限标识符列表
     */
    List<String> getCurrentUserPermissions();

    /**
     * 根据角色ID获取权限标识符列表
     */
    List<String> getPermissionsByRoleId(Long roleId);

    /**
     * 根据角色名获取权限标识符列表
     */
    List<String> getPermissionsByRoleName(String roleName);

    /**
     * 获取所有权限（用于角色管理界面）
     */
    List<PermissionResponse> getAllPermissions();

    /**
     * 获取指定角色的权限列表（带 assigned 标记）
     */
    List<PermissionResponse> getPermissionsWithAssigned(Long roleId);

    /**
     * 更新角色的权限
     */
    void updateRolePermissions(Long roleId, List<Long> permissionIds);
}
