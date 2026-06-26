package com.certification.backend.service.impl;

import com.certification.backend.dto.request.UserRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.UserProfileResponse;
import com.certification.backend.dto.response.UserResponse;
import com.certification.backend.entity.User;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.service.UserService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现类
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final String DEFAULT_PASSWORD = "123456";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserResponse> listUsers(String usernameFuzzy, String nameFuzzy,
                                              String role, Integer status, PageQuery pageQuery) {
        // 动态查询条件
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (usernameFuzzy != null && !usernameFuzzy.isEmpty()) {
                predicates.add(cb.like(root.get("username"), "%" + usernameFuzzy + "%"));
            }
            if (nameFuzzy != null && !nameFuzzy.isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + nameFuzzy + "%"));
            }
            if (role != null && !role.isEmpty()) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> page = userRepository.findAll(spec,
                PageRequest.of(pageQuery.getPageNum() - 1, pageQuery.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")));

        List<UserResponse> list = page.getContent().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());

        return new PageResult<>(page.getTotalElements(), pageQuery.getPageNum(),
                pageQuery.getPageSize(), list);
    }

    @Override
    public UserResponse addUser(UserRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD)); // 新增用户默认密码
        user.setName(request.getName());
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        userRepository.save(user);
        log.info("新增用户: username={}, role={}", user.getUsername(), user.getRole());
        return toUserResponse(user);
    }

    @Override
    public UserResponse updateUser(UserRequest request) {
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.USER_NOT_FOUND));

        // 如果修改了用户名，检查是否与其他用户冲突
        if (!user.getUsername().equals(request.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);
        }

        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        userRepository.save(user);
        log.info("编辑用户: id={}, username={}", user.getId(), user.getUsername());
        return toUserResponse(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.USER_NOT_FOUND));
        userRepository.delete(user);
        log.info("删除用户: id={}, username={}", id, user.getUsername());
    }

    @Override
    public void resetPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.USER_NOT_FOUND));
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userRepository.save(user);
        log.info("重置密码: id={}, username={}, defaultPassword={}",
                id, user.getUsername(), DEFAULT_PASSWORD);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.USER_NOT_FOUND));
        return toUserResponse(user);
    }

    @Override
    public int batchImport(List<User> users) {
        int successCount = 0;
        for (User user : users) {
            try {
                if (user.getUsername() == null || user.getUsername().isEmpty()) {
                    continue;
                }
                if (userRepository.existsByUsername(user.getUsername())) {
                    continue; // 跳过已存在的用户名
                }
                user.setPassword(passwordEncoder.encode(
                        user.getPassword() != null ? user.getPassword() : DEFAULT_PASSWORD));
                if (user.getStatus() == null) {
                    user.setStatus(1);
                }
                userRepository.save(user);
                successCount++;
            } catch (Exception e) {
                log.warn("批量导入跳过异常记录: username={}, error={}", user.getUsername(), e.getMessage());
            }
        }
        log.info("批量导入完成: 成功{}条", successCount);
        return successCount;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.USER_NOT_FOUND));
        return toProfileResponse(user);
    }

    /**
     * User Entity -> UserProfileResponse VO
     */
    private UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse resp = new UserProfileResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setName(user.getName());
        resp.setRole(user.getRole());
        resp.setPhone(user.getPhone());
        resp.setEmail(user.getEmail());
        resp.setDepartment(user.getDepartment());
        resp.setTitle(user.getTitle());
        resp.setOffice(user.getOffice());
        resp.setMajor(user.getMajor());
        resp.setGrade(user.getGrade());
        resp.setClassName(user.getClassName());
        return resp;
    }

    /**
     * User Entity -> UserResponse VO
     */
    private UserResponse toUserResponse(User user) {
        UserResponse resp = new UserResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setName(user.getName());
        resp.setRole(user.getRole());
        resp.setPhone(user.getPhone());
        resp.setEmail(user.getEmail());
        resp.setDepartment(user.getDepartment());
        resp.setStatus(user.getStatus());
        resp.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DTF) : null);
        return resp;
    }
}