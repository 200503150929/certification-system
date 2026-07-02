package com.certification.backend.controller;

import com.certification.backend.dto.response.UserProfileResponse;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.security.JwtUtil;
import com.certification.backend.security.UserDetailsServiceImpl;
import com.certification.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "student01")
    void profileReturnsCurrentUserProfile() throws Exception {
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(20L);
        profile.setUsername("student01");
        profile.setName("Student A");
        profile.setRole("student");
        profile.setMajor("Computer Science");
        profile.setGrade("2024");
        profile.setClassName("1");
        when(userService.getProfile("student01")).thenReturn(profile);

        mockMvc.perform(get("/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(20))
                .andExpect(jsonPath("$.data.username").value("student01"))
                .andExpect(jsonPath("$.data.role").value("student"))
                .andExpect(jsonPath("$.data.major").value("Computer Science"));

        verify(userService).getProfile("student01");
    }
}
