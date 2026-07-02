package com.certification.backend.service.impl;

import com.certification.backend.dto.request.LoginRequest;
import com.certification.backend.dto.response.LoginResponse;
import com.certification.backend.entity.User;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.security.JwtUtil;
import com.certification.backend.service.AuthService;
import com.certification.backend.service.PermissionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 认证服务实现类
 */
@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PermissionService permissionService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           PermissionService permissionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.permissionService = permissionService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 查询用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.LOGIN_FAILED));

        // 2. 校验状态
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCodeEnum.USER_DISABLED);
        }

        // 3. 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.LOGIN_FAILED);
        }

        // 4. 获取该角色的权限标识符列表
        List<String> permissions = permissionService.getPermissionsByRoleName(user.getRole());

        // 5. 生成 Token（含权限）
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole(), permissions);

        // 6. 组装响应
        return new LoginResponse(token, user.getId(), user.getUsername(),
                user.getName(), user.getRole(), permissions);
    }

    @Override
    @Transactional(readOnly = false)
    public void changePassword(String username, String oldPassword, String newPassword) {
        // 1. 查询用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.USER_NOT_FOUND));

        // 2. 校验原密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.OLD_PASSWORD_ERROR);
        }

        // 3. 新密码不能与原密码相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PASSWORD_RESET_FAILED, "新密码不能与旧密码相同");
        }

        // 4. 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
