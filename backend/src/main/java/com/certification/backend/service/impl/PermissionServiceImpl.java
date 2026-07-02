package com.certification.backend.service.impl;

import com.certification.backend.dto.response.PermissionResponse;
import com.certification.backend.entity.Permission;
import com.certification.backend.entity.RolePermission;
import com.certification.backend.entity.RolePermission;
import com.certification.backend.repository.PermissionRepository;
import com.certification.backend.repository.RolePermissionRepository;
import com.certification.backend.repository.RoleRepository;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.service.PermissionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 */
@Service
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public PermissionServiceImpl(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 PermissionRepository permissionRepository,
                                 RolePermissionRepository rolePermissionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    public List<String> getCurrentUserPermissions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(user -> getPermissionsByRoleName(user.getRole()))
                .orElse(Collections.emptyList());
    }

    @Override
    public List<String> getPermissionsByRoleId(Long roleId) {
        return rolePermissionRepository.findPermissionCodesByRoleId(roleId);
    }

    @Override
    public List<String> getPermissionsByRoleName(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .map(role -> rolePermissionRepository.findPermissionCodesByRoleId(role.getId()))
                .orElse(Collections.emptyList());
    }

    @Override
    public List<PermissionResponse> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = false)
    public void updateRolePermissions(Long roleId, List<Long> permissionIds) {
        // 先删除原有关联
        rolePermissionRepository.deleteByRoleId(roleId);

        // 批量插入新关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RolePermission> newRps = permissionIds.stream()
                    .map(permId -> {
                        RolePermission rp = new RolePermission();
                        rp.setRoleId(roleId);
                        rp.setPermissionId(permId);
                        return rp;
                    })
                    .collect(Collectors.toList());
            rolePermissionRepository.saveAll(newRps);
        }
    }

    /**
     * 获取指定角色的权限列表（带 assigned 标记），用于角色编辑界面
     */
    public List<PermissionResponse> getPermissionsWithAssigned(Long roleId) {
        Set<Long> assignedPermIds = rolePermissionRepository.findByRoleId(roleId)
                .stream()
                .map(rp -> rp.getPermissionId())
                .collect(Collectors.toSet());

        return permissionRepository.findAll().stream()
                .map(p -> {
                    PermissionResponse resp = toResponse(p);
                    resp.setAssigned(assignedPermIds.contains(p.getId()));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    private PermissionResponse toResponse(Permission p) {
        PermissionResponse resp = new PermissionResponse();
        resp.setId(p.getId());
        resp.setPermissionCode(p.getPermissionCode());
        resp.setPermissionName(p.getPermissionName());
        resp.setModule(p.getModule());
        resp.setSortOrder(p.getSortOrder());
        return resp;
    }
}
