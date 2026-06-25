package com.certification.backend.config;

import com.certification.backend.entity.Course;
import com.certification.backend.entity.Program;
import com.certification.backend.entity.Role;
import com.certification.backend.entity.User;
import com.certification.backend.repository.CourseRepository;
import com.certification.backend.repository.ProgramRepository;
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
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           ProgramRepository programRepository,
                           CourseRepository courseRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public void run(String... args) {
        // 1. 初始化角色
        initRoles();

        // 2. 初始化默认管理员账号
        initDefaultAdmin();

        // 3. 初始化测试教师账号（仅用于开发阶段测试）
        initTestTeacher();

        // 4. 初始化测试培养方案和课程（仅用于开发阶段测试）
        initTestData();
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

    private void initTestTeacher() {
        if (userRepository.findByUsername("teacher01").isEmpty()) {
            User teacher = new User();
            teacher.setUsername("teacher01");
            teacher.setPassword(passwordEncoder.encode("123456"));
            teacher.setName("测试教师");
            teacher.setRole("teacher");
            teacher.setPhone("13800000001");
            teacher.setDepartment("计算机学院");
            teacher.setStatus(1);
            userRepository.save(teacher);
            log.info("初始化测试教师账号: teacher01 / 123456");
        }
    }

    private void initTestData() {
        // 初始化测试培养方案
        if (programRepository.count() == 0) {
            Program program = new Program();
            program.setMajorName("计算机科学与技术");
            program.setVersion("2024");
            program.setStatus("published");
            program = programRepository.save(program);
            log.info("初始化测试培养方案: 计算机科学与技术 2024");

            // 初始化测试课程
            if (courseRepository.count() == 0) {
                Course course = new Course();
                course.setCode("CS101");
                course.setName("数据结构");
                course.setCredits(new java.math.BigDecimal("4.0"));
                course.setTotalHours(64);
                course.setTheoryHours(48);
                course.setLabHours(16);
                course.setSemester("2025-1");
                course.setCourseType("专业核心");
                course.setIsRequired(true);
                course.setProgramId(program.getId());
                courseRepository.save(course);
                log.info("初始化测试课程: CS101 数据结构 (id={})", course.getId());
            }
        }
    }
}