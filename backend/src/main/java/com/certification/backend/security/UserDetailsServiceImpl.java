package com.certification.backend.security;

import com.certification.backend.entity.User;
import com.certification.backend.repository.RolePermissionRepository;
import com.certification.backend.repository.RoleRepository;
import com.certification.backend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 从数据库加载用户信息，组装为 UserDetails
 *
 * 生成包含角色和权限标识符的 authorities 列表，
 * 支持 @PreAuthorize("hasRole('ADMIN')") 和 @PreAuthorize("hasAuthority('user:list')") 两种模式。
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public UserDetailsServiceImpl(UserRepository userRepository,
                                  RoleRepository roleRepository,
                                  RolePermissionRepository rolePermissionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 1. 角色权限（前缀 ROLE_ 是 Spring Security 约定）
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));

        // 2. 权限标识符（支持 hasAuthority 检查）
        roleRepository.findByRoleName(user.getRole())
                .ifPresent(role -> {
                    List<String> permissionCodes =
                            rolePermissionRepository.findPermissionCodesByRoleId(role.getId());
                    permissionCodes.forEach(code ->
                            authorities.add(new SimpleGrantedAuthority(code)));
                });

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getStatus() == 1,
                true,
                true,
                true,
                authorities
        );
    }
}
