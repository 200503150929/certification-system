package com.certification.backend.repository;

import com.certification.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 根据角色名查询
     */
    Optional<Role> findByRoleName(String roleName);
}