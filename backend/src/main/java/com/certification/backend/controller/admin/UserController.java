package com.certification.backend.controller.admin;

import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.request.UserRequest;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.dto.response.UserResponse;
import com.certification.backend.entity.User;
import com.certification.backend.service.UserService;
import com.certification.backend.util.ExcelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户管理控制器（管理员权限）
 */
@Tag(name = "02-用户管理", description = "系统用户增删改查、密码重置、Excel 批量导入")
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "分页查询用户列表",
            description = "支持按用户名、姓名模糊搜索，按角色、状态精确过滤")
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

    @Operation(summary = "查询用户详情")
    @GetMapping("/detail/{id}")
    public ResponseVO<UserResponse> detail(@PathVariable Long id) {
        return ResponseVO.success(userService.getUserById(id));
    }

    @Operation(summary = "新增用户", description = "新增用户默认密码为 123456")
    @PostMapping("/add")
    public ResponseVO<UserResponse> add(@Valid @RequestBody UserRequest request) {
        return ResponseVO.success(userService.addUser(request));
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/update")
    public ResponseVO<UserResponse> update(@Valid @RequestBody UserRequest request) {
        if (request.getId() == null) {
            return ResponseVO.error(400, "用户ID不能为空");
        }
        return ResponseVO.success(userService.updateUser(request));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete/{id}")
    public ResponseVO<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseVO.success();
    }

    @Operation(summary = "重置密码", description = "重置为默认密码 123456")
    @PutMapping("/reset-password/{id}")
    public ResponseVO<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return ResponseVO.success();
    }

    @Operation(summary = "批量导入用户（Excel）",
            description = "上传 Excel 文件批量导入用户，文件需包含：用户名、姓名、角色、电话、邮箱、院系")
    @PostMapping("/import")
    public ResponseVO<String> importUsers(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseVO.error(400, "上传文件不能为空");
        }
        List<User> users = ExcelUtil.readUsersFromExcel(file);
        int count = userService.batchImport(users);
        return ResponseVO.success("成功导入 " + count + " / " + users.size() + " 条记录");
    }
}