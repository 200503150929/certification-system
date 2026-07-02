package com.certification.backend.repository;

import com.certification.backend.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色-权限关联数据访问
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /** 根据角色ID查询所有关联记录 */
    List<RolePermission> findByRoleId(Long roleId);

    /** 根据角色ID列表查询所有关联记录 */
    List<RolePermission> findByRoleIdIn(List<Long> roleIds);

    /** 根据角色ID删除关联 */
    void deleteByRoleId(Long roleId);

    /** 查询指定角色的所有权限标识符 */
    @Query("SELECT p.permissionCode FROM Permission p JOIN RolePermission rp ON p.id = rp.permissionId WHERE rp.roleId = :roleId")
    List<String> findPermissionCodesByRoleId(@Param("roleId") Long roleId);
}
