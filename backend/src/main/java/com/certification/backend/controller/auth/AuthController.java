package com.certification.backend.controller.auth;

import com.certification.backend.dto.request.LoginRequest;
import com.certification.backend.dto.response.LoginResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证控制器：登录、修改密码接口
 */
@Tag(name = "01-认证管理", description = "用户登录、Token 认证、密码修改相关接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @Operation(summary = "用户登录", description = "校验用户名密码，返回 JWT Token 和用户基本信息")
    @PostMapping("/login")
    public ResponseVO<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseVO.success(response);
    }

    /**
     * 修改密码
     * POST /api/auth/changePassword
     * 所有角色（admin/teacher/student）均可调用
     */
    @Operation(summary = "修改密码", description = "所有用户均可修改自己的密码，需提供原密码验证")
    @PostMapping("/changePassword")
    public ResponseVO<Object> changePassword(@Valid @RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        // 参数校验
        if (oldPassword == null || oldPassword.isEmpty()) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "原密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "新密码不能为空");
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "确认密码不能为空");
        }
        if (!newPassword.equals(confirmPassword)) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "两次输入的新密码不一致");
        }

        // 从 SecurityContextHolder 获取当前用户名
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        authService.changePassword(username, oldPassword, newPassword);
        return ResponseVO.success("密码修改成功，请重新登录", null);
    }
}