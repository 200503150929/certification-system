package com.certification.backend.controller;

import com.certification.backend.dto.request.UpdateProfileRequest;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.dto.response.UserProfileResponse;
import com.certification.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


/**
 * 用户个人信息控制器（教师/学生/管理员通用）
 */
@Tag(name = "10-用户个人信息", description = "用户查看和更新个人资料")
@RestController
@RequestMapping("/user")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前登录用户的个人信息
     * GET /api/user/profile
     */
    @Operation(summary = "查看个人信息")
    @GetMapping("/profile")
    public ResponseVO<UserProfileResponse> profile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserProfileResponse response = userService.getProfile(username);
        return ResponseVO.success(response);
    }

    /**
     * 更新当前登录用户的个人信息
     * PUT /api/user/profile
     */
    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public ResponseVO<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserProfileResponse response = userService.updateProfile(username, request);
        return ResponseVO.success(response);
    }
}