package com.certification.backend.controller.admin;

import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.dto.response.RoleListResponse;
import com.certification.backend.entity.Role;
import com.certification.backend.repository.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理控制器（管理员权限）
 */
@Tag(name = "02-角色管理", description = "系统角色查询（权限分配在 /permissions 接口）")
@RestController
@RequestMapping("/admin/roles")
@PreAuthorize("hasAuthority('role:list')")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Operation(summary = "查询所有角色列表")
    @GetMapping("/list")
    public ResponseVO<List<RoleListResponse>> listRoles() {
        List<Role> roles = roleRepository.findAll();
        List<RoleListResponse> result = roles.stream()
                .map(r -> new RoleListResponse(r.getId(), r.getRoleName()))
                .collect(Collectors.toList());
        return ResponseVO.success(result);
    }
}
