package com.certification.backend.controller.admin;

import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.request.UserRequest;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.dto.response.UserResponse;
import com.certification.backend.service.UserService;
import com.certification.backend.util.ExcelUtil;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户管理控制器（管理员权限）
 */
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 分页查询用户列表
     * GET /api/admin/users/list
     */
    @GetMapping("/list")
    public ResponseVO<PageResult<UserResponse>> listUsers(
            @RequestParam(required = false) String usernameFuzzy,
            @RequestParam(required = false) String nameFuzzy,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @Valid PageQuery pageQuery) {
        PageResult<UserResponse> result = userService.listUsers(
                usernameFuzzy, nameFuzzy, role, status, pageQuery);
        return ResponseVO.success(result);
    }

    /**
     * 查询用户详情
     * GET /api/admin/users/detail/{id}
     */
    @GetMapping("/detail/{id}")
    public ResponseVO<UserResponse> detail(@PathVariable Long id) {
        return ResponseVO.success(userService.getUserById(id));
    }

    /**
     * 新增用户
     * POST /api/admin/users/add
     */
    @PostMapping("/add")
    public ResponseVO<UserResponse> add(@Valid @RequestBody UserRequest request) {
        return ResponseVO.success(userService.addUser(request));
    }

    /**
     * 编辑用户
     * PUT /api/admin/users/update
     */
    @PutMapping("/update")
    public ResponseVO<UserResponse> update(@Valid @RequestBody UserRequest request) {
        if (request.getId() == null) {
            return ResponseVO.error(400, "用户ID不能为空");
        }
        return ResponseVO.success(userService.updateUser(request));
    }

    /**
     * 删除用户
     * DELETE /api/admin/users/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public ResponseVO<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseVO.success();
    }

    /**
     * 重置密码（重置为默认密码 123456）
     * PUT /api/admin/users/reset-password/{id}
     */
    @PutMapping("/reset-password/{id}")
    public ResponseVO<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return ResponseVO.success();
    }

    /**
     * 批量导入用户（Excel）
     * POST /api/admin/users/import
     */
    @PostMapping("/import")
    public ResponseVO<String> importUsers(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseVO.error(400, "上传文件不能为空");
        }
        List<com.certification.backend.entity.User> users = ExcelUtil.readUsersFromExcel(file);
        int count = userService.batchImport(users);
        return ResponseVO.success("成功导入 " + count + " / " + users.size() + " 条记录");
    }
}