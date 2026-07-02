package com.certification.backend.controller.admin;

import com.certification.backend.dto.request.UserRequest;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.UserResponse;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.security.JwtUtil;
import com.certification.backend.security.UserDetailsServiceImpl;
import com.certification.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "admin", roles = "ADMIN")
class UserControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void listUsersReturnsPagedResult() throws Exception {
        UserResponse user = userResponse(1L, "teacher01", "Teacher A", "teacher");
        when(userService.listUsers(eq("teach"), eq("Teacher"), eq("teacher"), eq(1), any()))
                .thenReturn(new PageResult<>(1, 1, 10, List.of(user)));

        mockMvc.perform(get("/admin/users/list")
                        .param("usernameFuzzy", "teach")
                        .param("nameFuzzy", "Teacher")
                        .param("role", "teacher")
                        .param("status", "1")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].username").value("teacher01"));
    }

    @Test
    void detailReturnsUserById() throws Exception {
        when(userService.getUserById(1L)).thenReturn(userResponse(1L, "admin", "Admin", "admin"));

        mockMvc.perform(get("/admin/users/detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void addReturnsCreatedUserResponse() throws Exception {
        when(userService.addUser(any(UserRequest.class)))
                .thenReturn(userResponse(2L, "student01", "Student A", "student"));

        mockMvc.perform(post("/admin/users/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest(null, "student01", "Student A", "student"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.role").value("student"));
    }

    @Test
    void addRejectsInvalidUsernameBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/admin/users/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest(null, "ab", "Student A", "student"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void updateRejectsMissingId() throws Exception {
        mockMvc.perform(put("/admin/users/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest(null, "student01", "Student A", "student"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void deleteDelegatesToService() throws Exception {
        mockMvc.perform(delete("/admin/users/delete/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(userService).deleteUser(5L);
    }

    @Test
    void resetPasswordDelegatesToService() throws Exception {
        mockMvc.perform(put("/admin/users/reset-password/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(userService).resetPassword(6L);
    }

    private static UserRequest userRequest(Long id, String username, String name, String role) {
        UserRequest request = new UserRequest();
        request.setId(id);
        request.setUsername(username);
        request.setName(name);
        request.setRole(role);
        request.setStatus(1);
        return request;
    }

    private static UserResponse userResponse(Long id, String username, String name, String role) {
        UserResponse response = new UserResponse();
        response.setId(id);
        response.setUsername(username);
        response.setName(name);
        response.setRole(role);
        response.setStatus(1);
        return response;
    }
}
