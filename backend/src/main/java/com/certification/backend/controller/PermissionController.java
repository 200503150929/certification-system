package com.certification.backend.controller;

import com.certification.backend.dto.response.PermissionResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.service.impl.PermissionServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 权限管理控制器
 *
 * 提供当前用户权限查询、所有权限列表、角色权限分配接口。
 */
@Tag(name = "00-权限管理", description = "权限标识符查询、角色权限分配接口")
@RestController
@RequestMapping("/permissions")
public class PermissionController {

    private final PermissionServiceImpl permissionService;

    public PermissionController(PermissionServiceImpl permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 获取当前登录用户的权限标识符列表
     * GET /api/permissions
     *
     * 登录成功后前端调用此接口获取该用户的权限列表，
     * 用于前端 v-permission 指令和路由守卫的权限判断。
     */
    @Operation(summary = "获取当前用户权限列表")
    @GetMapping
    public ResponseVO<List<String>> getCurrentUserPermissions() {
        List<String> permissions = permissionService.getCurrentUserPermissions();
        return ResponseVO.success(permissions);
    }

    /**
     * 获取所有权限（带角色分配标记）
     * GET /api/permissions/all?roleId={roleId}
     *
     * 管理员在角色权限编辑界面调用，查看所有可分配的权限。
     */
    @Operation(summary = "获取所有权限列表（带分配标记）")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('role:list')")
    public ResponseVO<List<PermissionResponse>> getAllPermissions(@RequestParam(required = false) Long roleId) {
        if (roleId != null) {
            return ResponseVO.success(permissionService.getPermissionsWithAssigned(roleId));
        }
        return ResponseVO.success(permissionService.getAllPermissions());
    }

    /**
     * 更新角色权限
     * PUT /api/permissions/role/{roleId}
     */
    @Operation(summary = "更新角色权限")
    @PutMapping("/role/{roleId}")
    @PreAuthorize("hasAuthority('role:update')")
    public ResponseVO<Void> updateRolePermissions(@PathVariable Long roleId,
                                                  @RequestBody Map<String, List<Long>> body) {
        List<Long> permissionIds = body.get("permissionIds");
        permissionService.updateRolePermissions(roleId, permissionIds);
        return ResponseVO.success("角色权限更新成功", null);
    }
}