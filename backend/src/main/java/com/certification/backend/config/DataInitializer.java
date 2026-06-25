package com.certification.backend.config;

import com.certification.backend.entity.*;
import com.certification.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 项目启动时初始化完整测试数据
 *
 * ====== 用户 ======
 * 管理员: admin / 123456
 * 教师: teacher01 / 123456
 * 学生: student01 / 123456, student02 / 123456, student03 / 123456
 *
 * ====== 培养方案 ======
 * 计算机科学与技术 2024版 (已发布)
 *
 * ====== 毕业要求 ======
 * GR1 "工程知识" (code=1)
 * GR2 "问题分析" (code=2)
 * GR3 "设计/开发解决方案" (code=3)
 *
 * ====== 指标点 ======
 * IP1: code=1.1, 掌基础知识, weight=0.25, passScore=0.60
 * IP2: code=1.2, 算法分析能力, weight=0.20, passScore=0.60
 * IP3: code=2.1, 数据结构应用, weight=0.15, passScore=0.60
 *
 * ====== 课程 ======
 * CS101 数据结构 (4.0学分, 专业核心, 必修)
 *
 * ====== 开课记录 ======
 * OF1: CS101, teacher01, 2025-2026, 第1学期
 *
 * ====== 课程目标 ======
 * CO1: 掌握基本数据结构 (weight=0.40)
 * CO2: 理解算法复杂度分析 (weight=0.30)
 * CO3: 能够设计高效算法 (weight=0.30)
 *
 * ====== 考核环节 ======
 * AS1: 平时作业 (weight=0.20, →CO1)
 * AS2: 实验报告 (weight=0.20, →CO1)
 * AS3: 期中考试 (weight=0.30, →CO2)
 * AS4: 期末考试 (weight=0.30, →CO3)
 *
 * ====== 成绩 (学生各考核环节得分) ======
 * student01: 85, 90, 78, 82
 * student02: 92, 88, 85, 90
 * student03: 75, 80, 70, 76
 *
 * ====== 课程目标-指标点支撑矩阵 ======
 * CO1 → IP1 (H), CO1 → IP2 (M)
 * CO2 → IP1 (H), CO2 → IP2 (M)
 * CO3 → IP3 (H)
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final GraduationRequirementRepository graduationRequirementRepository;
    private final IndicatorPointRepository indicatorPointRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseObjectiveRepository courseObjectiveRepository;
    private final AssessmentRepository assessmentRepository;
    private final GradeRepository gradeRepository;
    private final ObjectiveIndicatorMatrixRepository oiMatrixRepository;
    private final StudentCourseRepository studentCourseRepository;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           ProgramRepository programRepository,
                           CourseRepository courseRepository,
                           GraduationRequirementRepository graduationRequirementRepository,
                           IndicatorPointRepository indicatorPointRepository,
                           CourseOfferingRepository courseOfferingRepository,
                           CourseObjectiveRepository courseObjectiveRepository,
                           AssessmentRepository assessmentRepository,
                           GradeRepository gradeRepository,
                           ObjectiveIndicatorMatrixRepository oiMatrixRepository,
                           StudentCourseRepository studentCourseRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.graduationRequirementRepository = graduationRequirementRepository;
        this.indicatorPointRepository = indicatorPointRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.courseObjectiveRepository = courseObjectiveRepository;
        this.assessmentRepository = assessmentRepository;
        this.gradeRepository = gradeRepository;
        this.oiMatrixRepository = oiMatrixRepository;
        this.studentCourseRepository = studentCourseRepository;
    }

    @Override
    public void run(String... args) {
        log.info("========== 开始初始化测试数据 ==========");

        initRoles();
        User admin = initAdmin();
        User teacher = initTeacher();
        List<User> students = initStudents();
        Program program = initProgram();
        List<GraduationRequirement> requirements = initGraduationRequirements(program);
        List<IndicatorPoint> indicators = initIndicatorPoints(requirements);
        Course course = initCourse(program);
        CourseOffering offering = initOffering(course, teacher);
        initStudentCourses(offering, students);
        List<CourseObjective> objectives = initObjectives(offering);
        List<Assessment> assessments = initAssessments(offering, objectives);
        initGrades(assessments, students);
        initObjectiveIndicatorMatrix(objectives, indicators);

        log.info("========== 测试数据初始化完成 ==========");
    }

    // ==================== 角色 ====================

    private void initRoles() {
        for (String name : new String[]{"admin", "teacher", "student"}) {
            if (roleRepository.findByRoleName(name).isEmpty()) {
                Role role = new Role();
                role.setRoleName(name);
                roleRepository.save(role);
                log.info("  ✓ 初始化角色: {}", name);
            }
        }
    }

    // ==================== 管理员 ====================

    private User initAdmin() {
        return userRepository.findByUsername("admin").orElseGet(() -> {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setName("系统管理员");
            admin.setRole("admin");
            admin.setStatus(1);
            User saved = userRepository.save(admin);
            log.info("  ✓ 管理员: admin / 123456");
            return saved;
        });
    }

    // ==================== 教师 ====================

    private User initTeacher() {
        return userRepository.findByUsername("teacher01").orElseGet(() -> {
            User teacher = new User();
            teacher.setUsername("teacher01");
            teacher.setPassword(passwordEncoder.encode("123456"));
            teacher.setName("张教授");
            teacher.setRole("teacher");
            teacher.setPhone("13800000001");
            teacher.setEmail("zhang@university.edu");
            teacher.setDepartment("计算机学院");
            teacher.setStatus(1);
            User saved = userRepository.save(teacher);
            log.info("  ✓ 教师: teacher01 / 123456 (张教授)");
            return saved;
        });
    }

    // ==================== 学生 ====================

    private List<User> initStudents() {
        List<User> students = new ArrayList<>();
        String[][] data = {
                {"student01", "张三"},
                {"student02", "李四"},
                {"student03", "王五"}
        };
        for (String[] d : data) {
            User s = userRepository.findByUsername(d[0]).orElseGet(() -> {
                User student = new User();
                student.setUsername(d[0]);
                student.setPassword(passwordEncoder.encode("123456"));
                student.setName(d[1]);
                student.setRole("student");
                student.setDepartment("计算机学院");
                student.setStatus(1);
                return userRepository.save(student);
            });
            students.add(s);
        }
        log.info("  ✓ 学生账号: student01, student02, student03 / 123456");
        return students;
    }

    // ==================== 培养方案 ====================

    private Program initProgram() {
        return programRepository.findByMajorName("计算机科学与技术").orElseGet(() -> {
            Program p = new Program();
            p.setMajorName("计算机科学与技术");
            p.setVersion("2024");
            p.setStatus("published");
            Program saved = programRepository.save(p);
            log.info("  ✓ 培养方案: 计算机科学与技术 2024版 (已发布, id={})", saved.getId());
            return saved;
        });
    }

    // ==================== 毕业要求 ====================

    private List<GraduationRequirement> initGraduationRequirements(Program program) {
        List<GraduationRequirement> existing = graduationRequirementRepository.findByProgramId(program.getId());
        if (!existing.isEmpty()) {
            log.info("  - 毕业要求已存在，跳过");
            return existing;
        }

        List<GraduationRequirement> list = new ArrayList<>();
        String[][] data = {
                {"1", "工程知识：能够将数学、自然科学、工程基础和专业知识用于解决计算机领域的复杂工程问题"},
                {"2", "问题分析：能够应用数学、自然科学和工程科学的基本原理，识别、表达并通过文献研究分析复杂工程问题"},
                {"3", "设计/开发解决方案：能够设计针对复杂工程问题的解决方案，设计满足特定需求的系统、单元或流程"}
        };
        for (String[] d : data) {
            GraduationRequirement gr = new GraduationRequirement();
            gr.setProgramId(program.getId());
            gr.setCode(d[0]);
            gr.setDescription(d[1]);
            list.add(graduationRequirementRepository.save(gr));
        }
        log.info("  ✓ 毕业要求: 3条 (GR1~GR3)");
        return list;
    }

    // ==================== 指标点 ====================

    private List<IndicatorPoint> initIndicatorPoints(List<GraduationRequirement> requirements) {
        // 检查是否已存在
        if (indicatorPointRepository.count() > 0) {
            log.info("  - 指标点已存在，跳过");
            return indicatorPointRepository.findAll();
        }

        List<IndicatorPoint> list = new ArrayList<>();

        // GR1 工程知识 → 2个指标点
        IndicatorPoint ip1 = new IndicatorPoint();
        ip1.setRequirementId(requirements.get(0).getId());
        ip1.setCode("1.1");
        ip1.setDescription("掌握计算机科学与技术领域的基础知识，包括离散数学、程序设计、数据结构等核心概念");
        ip1.setWeight(new BigDecimal("0.25"));
        ip1.setPassScore(new BigDecimal("0.60"));
        list.add(indicatorPointRepository.save(ip1));

        IndicatorPoint ip2 = new IndicatorPoint();
        ip2.setRequirementId(requirements.get(0).getId());
        ip2.setCode("1.2");
        ip2.setDescription("具备算法设计与分析能力，能够运用数学方法分析算法的时间复杂度和空间复杂度");
        ip2.setWeight(new BigDecimal("0.20"));
        ip2.setPassScore(new BigDecimal("0.60"));
        list.add(indicatorPointRepository.save(ip2));

        // GR2 问题分析 → 1个指标点
        IndicatorPoint ip3 = new IndicatorPoint();
        ip3.setRequirementId(requirements.get(1).getId());
        ip3.setCode("2.1");
        ip3.setDescription("能够运用数据结构和算法知识，识别和分析实际工程问题的核心要素");
        ip3.setWeight(new BigDecimal("0.15"));
        ip3.setPassScore(new BigDecimal("0.60"));
        list.add(indicatorPointRepository.save(ip3));

        // GR3 设计/开发解决方案 → 1个指标点
        IndicatorPoint ip4 = new IndicatorPoint();
        ip4.setRequirementId(requirements.get(2).getId());
        ip4.setCode("3.1");
        ip4.setDescription("能够针对具体需求，选择合适的数据结构和算法，设计并实现高效的解决方案");
        ip4.setWeight(new BigDecimal("0.15"));
        ip4.setPassScore(new BigDecimal("0.60"));
        list.add(indicatorPointRepository.save(ip4));

        log.info("  ✓ 指标点: 4个 (1.1, 1.2, 2.1, 3.1)");
        return list;
    }

    // ==================== 课程 ====================

    private Course initCourse(Program program) {
        return courseRepository.findByCode("CS101").orElseGet(() -> {
            Course c = new Course();
            c.setCode("CS101");
            c.setName("数据结构");
            c.setCredits(new BigDecimal("4.0"));
            c.setTotalHours(64);
            c.setTheoryHours(48);
            c.setLabHours(16);
            c.setSemester("2025-1");
            c.setCourseType("专业核心");
            c.setIsRequired(true);
            c.setProgramId(program.getId());
            Course saved = courseRepository.save(c);
            log.info("  ✓ 课程: CS101 数据结构 (4.0学分, 专业核心必修, id={})", saved.getId());
            return saved;
        });
    }

    // ==================== 开课记录 ====================

    private CourseOffering initOffering(Course course, User teacher) {
        // 检查是否已有该教师该课程的开课记录
        boolean exists = courseOfferingRepository
                .existsByCourseIdAndTeacherIdAndAcademicYearAndSemester(
                        course.getId(), teacher.getId(), "2025-2026", "第1学期");
        if (exists) {
            log.info("  - 开课记录已存在，跳过");
            return courseOfferingRepository.findByTeacherId(teacher.getId()).get(0);
        }

        CourseOffering of = new CourseOffering();
        of.setCourseId(course.getId());
        of.setTeacherId(teacher.getId());
        of.setAcademicYear("2025-2026");
        of.setSemester("第1学期");
        CourseOffering saved = courseOfferingRepository.save(of);
        log.info("  ✓ 开课记录: CS101, teacher01, 2025-2026 第1学期 (id={})", saved.getId());
        return saved;
    }

    // ==================== 学生选课 ====================

    private void initStudentCourses(CourseOffering offering, List<User> students) {
        if (studentCourseRepository.existsByOfferingId(offering.getId())) {
            log.info("  - 学生选课记录已存在，跳过");
            return;
        }
        for (User student : students) {
            StudentCourse sc = new StudentCourse();
            sc.setOfferingId(offering.getId());
            sc.setStudentId(student.getId());
            studentCourseRepository.save(sc);
        }
        log.info("  ✓ 学生选课: 3人已加入开课");
    }

    // ==================== 课程目标 ====================

    private List<CourseObjective> initObjectives(CourseOffering offering) {
        List<CourseObjective> existing = courseObjectiveRepository.findByOfferingId(offering.getId());
        if (!existing.isEmpty()) {
            log.info("  - 课程目标已存在，跳过");
            return existing;
        }

        List<CourseObjective> list = new ArrayList<>();
        String[][] data = {
                {"掌握线性表、树、图等基本数据结构的定义、存储结构和基本操作", "0.40"},
                {"理解算法时间复杂度和空间复杂度的分析方法，能够对常见算法进行效率评估", "0.30"},
                {"具备根据实际问题选择合适数据结构并设计高效算法的能力", "0.30"}
        };
        for (String[] d : data) {
            CourseObjective obj = new CourseObjective();
            obj.setOfferingId(offering.getId());
            obj.setDescription(d[0]);
            obj.setWeight(new BigDecimal(d[1]));
            list.add(courseObjectiveRepository.save(obj));
        }
        log.info("  ✓ 课程目标: 3个 (CO1~CO3)");
        return list;
    }

    // ==================== 考核环节 ====================

    private List<Assessment> initAssessments(CourseOffering offering, List<CourseObjective> objectives) {
        List<Assessment> existing = assessmentRepository.findByOfferingId(offering.getId());
        if (!existing.isEmpty()) {
            log.info("  - 考核环节已存在，跳过");
            return existing;
        }

        List<Assessment> list = new ArrayList<>();
        // [名称, 权重, 关联课程目标索引]
        Object[][] data = {
                {"平时作业", "0.20", 0},   // → CO1
                {"实验报告", "0.20", 0},   // → CO1
                {"期中考试", "0.30", 1},   // → CO2
                {"期末考试", "0.30", 2}    // → CO3
        };
        for (Object[] d : data) {
            Assessment as = new Assessment();
            as.setOfferingId(offering.getId());
            as.setName((String) d[0]);
            as.setWeight(new BigDecimal((String) d[1]));
            as.setObjectiveId(objectives.get((int) d[2]).getId());
            list.add(assessmentRepository.save(as));
        }
        log.info("  ✓ 考核环节: 4个 (平时作业, 实验报告, 期中考试, 期末考试)");
        return list;
    }

    // ==================== 成绩 ====================

    private void initGrades(List<Assessment> assessments, List<User> students) {
        // 检查是否已有成绩
        List<Long> assessIds = assessments.stream().map(Assessment::getId).toList();
        if (!gradeRepository.findByAssessmentIdIn(assessIds).isEmpty()) {
            log.info("  - 成绩数据已存在，跳过");
            return;
        }

        // student01: [85, 90, 78, 82]
        // student02: [92, 88, 85, 90]
        // student03: [75, 80, 70, 76]
        int[][] scores = {
                {85, 90, 78, 82},
                {92, 88, 85, 90},
                {75, 80, 70, 76}
        };

        int totalGrades = 0;
        for (int i = 0; i < students.size(); i++) {
            for (int j = 0; j < assessments.size(); j++) {
                Grade g = new Grade();
                g.setAssessmentId(assessments.get(j).getId());
                g.setStudentId(students.get(i).getId());
                g.setScore(new BigDecimal(scores[i][j]));
                gradeRepository.save(g);
                totalGrades++;
            }
        }
        log.info("  ✓ 成绩: {} 条 (3名学生 × 4个考核环节)", totalGrades);
    }

    // ==================== 课程目标-指标点支撑矩阵 ====================

    private void initObjectiveIndicatorMatrix(List<CourseObjective> objectives,
                                               List<IndicatorPoint> indicators) {
        if (oiMatrixRepository.count() > 0) {
            log.info("  - 支撑矩阵已存在，跳过");
            return;
        }

        // CO1 → IP1(H), CO1 → IP2(M)
        // CO2 → IP1(H), CO2 → IP2(M)
        // CO3 → IP3(H)
        String[][] matrix = {
                {"0", "0", "H"},  // CO1 → IP1
                {"0", "1", "M"},  // CO1 → IP2
                {"1", "0", "H"},  // CO2 → IP1
                {"1", "1", "M"},  // CO2 → IP2
                {"2", "2", "H"}   // CO3 → IP3
        };

        for (String[] m : matrix) {
            ObjectiveIndicatorMatrix oim = new ObjectiveIndicatorMatrix();
            oim.setObjectiveId(objectives.get(Integer.parseInt(m[0])).getId());
            oim.setIndicatorId(indicators.get(Integer.parseInt(m[1])).getId());
            oim.setSupportLevel(m[2]);
            oiMatrixRepository.save(oim);
        }
        log.info("  ✓ 支撑矩阵: 5条 (课程目标→指标点)");
    }
}