package com.certification.backend.service.impl;

import com.certification.backend.dto.request.LoginRequest;
import com.certification.backend.dto.response.LoginResponse;
import com.certification.backend.entity.User;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void loginReturnsResponseWhenCredentialsAreValid() {
        User user = user(1L, "teacher01", "Teacher A", "teacher", 1, "encoded");
        LoginRequest request = loginRequest("teacher01", "123456");
        when(userRepository.findByUsername("teacher01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken("teacher01", "teacher")).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("teacher01");
        assertThat(response.getName()).isEqualTo("Teacher A");
        assertThat(response.getRole()).isEqualTo("teacher");
    }

    @Test
    void loginThrowsLoginFailedWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertBusinessException(() -> authService.login(loginRequest("missing", "123456")),
                ResultCodeEnum.LOGIN_FAILED);
    }

    @Test
    void loginThrowsUserDisabledBeforePasswordCheck() {
        User user = user(2L, "student01", "Student A", "student", 0, "encoded");
        when(userRepository.findByUsername("student01")).thenReturn(Optional.of(user));

        assertBusinessException(() -> authService.login(loginRequest("student01", "123456")),
                ResultCodeEnum.USER_DISABLED);
        verify(passwordEncoder, never()).matches("123456", "encoded");
    }

    @Test
    void loginThrowsLoginFailedWhenPasswordDoesNotMatch() {
        User user = user(3L, "admin", "Admin", "admin", 1, "encoded");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "encoded")).thenReturn(false);

        assertBusinessException(() -> authService.login(loginRequest("admin", "bad")),
                ResultCodeEnum.LOGIN_FAILED);
        verify(jwtUtil, never()).generateToken("admin", "admin");
    }

    @Test
    void changePasswordSavesEncodedNewPassword() {
        User user = user(4L, "teacher02", "Teacher B", "teacher", 1, "old-encoded");
        when(userRepository.findByUsername("teacher02")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-pass", "old-encoded")).thenReturn(true);
        when(passwordEncoder.matches("new-pass", "old-encoded")).thenReturn(false);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-encoded");

        authService.changePassword("teacher02", "old-pass", "new-pass");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("new-encoded");
    }

    @Test
    void changePasswordThrowsWhenOldPasswordIsWrong() {
        User user = user(5L, "student02", "Student B", "student", 1, "old-encoded");
        when(userRepository.findByUsername("student02")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-encoded")).thenReturn(false);

        assertBusinessException(() -> authService.changePassword("student02", "wrong", "new-pass"),
                ResultCodeEnum.OLD_PASSWORD_ERROR);
        verify(userRepository, never()).save(user);
    }

    @Test
    void changePasswordThrowsWhenNewPasswordMatchesOldPassword() {
        User user = user(6L, "student03", "Student C", "student", 1, "old-encoded");
        when(userRepository.findByUsername("student03")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-pass", "old-encoded")).thenReturn(true);
        when(passwordEncoder.matches("same-pass", "old-encoded")).thenReturn(true);

        assertBusinessException(() -> authService.changePassword("student03", "old-pass", "same-pass"),
                ResultCodeEnum.PASSWORD_RESET_FAILED);
        verify(userRepository, never()).save(user);
    }

    private static LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private static User user(Long id, String username, String name, String role, Integer status, String password) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setName(name);
        user.setRole(role);
        user.setStatus(status);
        user.setPassword(password);
        return user;
    }

    private static void assertBusinessException(Runnable action, ResultCodeEnum expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(expected.getCode());
                });
    }
}
