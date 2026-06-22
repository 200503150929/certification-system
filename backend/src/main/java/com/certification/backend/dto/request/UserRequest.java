package com.certification.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 新增/编辑用户请求参数
 */
@Setter
@Getter
public class UserRequest {

    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,12}$", message = "用户名必须为3-12位字母、数字或下划线")
    private String username;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "角色不能为空")
    private String role; // admin, teacher, student

    private String phone;

    private String email;

    private String department;

    private Integer status;

}