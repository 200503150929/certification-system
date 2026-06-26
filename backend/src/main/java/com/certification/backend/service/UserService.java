package com.certification.backend.service;

import com.certification.backend.dto.request.UserRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.UserProfileResponse;
import com.certification.backend.dto.response.UserResponse;
import com.certification.backend.entity.User;

import java.util.List;

/**
 * 用户管理服务接口
 */
public interface UserService {

    /**
     * 分页查询用户列表
     */
    PageResult<UserResponse> listUsers(String usernameFuzzy, String nameFuzzy,
                                       String role, Integer status, PageQuery pageQuery);

    /**
     * 新增用户（密码加密）
     */
    UserResponse addUser(UserRequest request);

    /**
     * 编辑用户
     */
    UserResponse updateUser(UserRequest request);

    /**
     * 删除用户
     */
    void deleteUser(Long id);

    /**
     * 重置密码为默认密码
     */
    void resetPassword(Long id);

    /**
     * 批量导入用户（Excel）
     */
    int batchImport(List<User> users);

    /**
     * 根据 ID 查询用户
     */
    UserResponse getUserById(Long id);

    /**
     * 根据用户名获取用户个人信息（教师/学生端查看个人资料）
     *
     * @param username 用户名（工号/学号）
     * @return 用户个人信息
     */
    UserProfileResponse getProfile(String username);

}
