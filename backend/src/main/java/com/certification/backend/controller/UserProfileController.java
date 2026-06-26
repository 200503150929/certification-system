package com.certification.backend.controller;

import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.dto.response.UserProfileResponse;
import com.certification.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户个人信息控制器（教师/学生通用）
 */
@Tag(name = "10-用户个人信息", description = "教师和学生查看个人资料")
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
    @Operation(summary = "查看个人信息", description = "根据当前登录的 Token 返回用户个人信息（教师和学生通用）")
    @GetMapping("/profile")
    public ResponseVO<UserProfileResponse> profile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserProfileResponse response = userService.getProfile(username);
        return ResponseVO.success(response);
    }
}
