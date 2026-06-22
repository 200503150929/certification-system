package com.certification.backend.config;

import com.certification.backend.entity.Role;
import com.certification.backend.entity.User;
import com.certification.backend.repository.RoleRepository;
import com.certification.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 项目启动时初始化基础数据：角色 + 默认管理员账号
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. 初始化角色
        initRoles();

        // 2. 初始化默认管理员账号
        initDefaultAdmin();
    }

    private void initRoles() {
        String[] roleNames = {"admin", "teacher", "student"};
        for (String roleName : roleNames) {
            if (roleRepository.findByRoleName(roleName).isEmpty()) {
                Role role = new Role();
                role.setRoleName(roleName);
                roleRepository.save(role);
                log.info("初始化角色: {}", roleName);
            }
        }
    }

    private void initDefaultAdmin() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setName("系统管理员");
            admin.setRole("admin");
            admin.setStatus(1);
            userRepository.save(admin);
            log.info("初始化默认管理员账号: admin / 123456");
        }
    }
}