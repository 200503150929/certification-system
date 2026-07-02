package com.certification.backend.controller.auth;

import com.certification.backend.dto.request.LoginRequest;
import com.certification.backend.dto.response.LoginResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.service.AuthService;
import com.certification.backend.service.LoginAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器：登录、修改密码接口
 */
@Tag(name = "01-认证管理", description = "用户登录、Token 认证、密码修改相关接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthService authService, LoginAttemptService loginAttemptService) {
        this.authService = authService;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @Operation(summary = "用户登录", description = "校验用户名密码，返回 JWT Token 和用户基本信息")
    @PostMapping("/login")
    public ResponseVO<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);

        // 检查是否被锁定
        if (loginAttemptService.isLocked(ipAddress)) {
            long remainingMinutes = loginAttemptService.getRemainingLockMinutes(ipAddress);
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(),
                    String.format("登录尝试次数过多，账号已被锁定，请 %d 分钟后重试", remainingMinutes));
        }

        try {
            LoginResponse response = authService.login(request);
            // 登录成功，清除失败记录
            loginAttemptService.resetAttempts(ipAddress);
            return ResponseVO.success(response);
        } catch (BusinessException e) {
            // 登录失败，记录失败次数
            loginAttemptService.recordFailedAttempt(ipAddress);
            throw e;
        }
    }

    /**
     * 获取当前 IP 的登录锁定状态（供前端查询）
     * GET /api/auth/lock-status
     */
    @Operation(summary = "获取锁定状态", description = "获取当前 IP 的登录失败次数和锁定状态")
    @GetMapping("/lock-status")
    public ResponseVO<Map<String, Object>> getLockStatus(HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        Map<String, Object> result = new HashMap<>();
        result.put("isLocked", loginAttemptService.isLocked(ipAddress));
        result.put("attemptCount", loginAttemptService.getAttemptCount(ipAddress));
        result.put("remainingMinutes", loginAttemptService.getRemainingLockMinutes(ipAddress));
        return ResponseVO.success(result);
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

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}