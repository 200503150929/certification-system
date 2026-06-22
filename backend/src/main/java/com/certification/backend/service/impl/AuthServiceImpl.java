package com.certification.backend.service.impl;

import com.certification.backend.dto.request.LoginRequest;
import com.certification.backend.dto.response.LoginResponse;
import com.certification.backend.entity.User;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.security.JwtUtil;
import com.certification.backend.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现类
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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

        // 4. 生成 Token
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        // 5. 组装响应
        return new LoginResponse(token, user.getId(), user.getUsername(),
                user.getName(), user.getRole());
    }
}