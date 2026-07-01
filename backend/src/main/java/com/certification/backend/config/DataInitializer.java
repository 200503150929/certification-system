package com.certification.backend.config;

import com.certification.backend.entity.*;
import com.certification.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 项目启动时初始化全面测试数据（答辩演示用）
 *
 * ====== 用户账号 ======
 * 管理员: admin / 123456
 * 教师:   teacher01 / 123456 (张教授-计算机学院)
 *         teacher02 / 123456 (李教授-计算机学院)
 *         teacher03 / 123456 (王教授-软件学院)
 *         teacher04 / 123456 (赵教授-软件学院)
 * 学生:   student01~15 / 123456
 *
 * ====== 培养方案 (2个专业) ======
 * 计算机科学与技术 2024版 (已发布)
 * 软件工程 2024版 (已发布)
 *
 * ====== 培养目标 (每专业3条) ======
 * ====== 毕业要求 (每专业12条，对标工程教育认证标准) ======
 * ====== 指标点 (每条毕业要求3个，共72个) ======
 * ====== 课程 (8门) ======
 * ====== 开课记录 (2个学期，共8条) ======
 * ====== 全部支撑矩阵 ======
 * ====== 学生成绩 (15人 × 8门课) ======
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    // ==================== 注入所有 Repository ====================
    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final ProgramRepository programRepo;
    private final EducationalObjectiveRepository objectiveRepo;
    private final GraduationRequirementRepository requirementRepo;
    private final IndicatorPointRepository indicatorRepo;
    private final ObjectiveRequirementMatrixRepository orMatrixRepo;
    private final CourseRepository courseRepo;
    private final CourseRequirementMatrixRepository crMatrixRepo;
    private final CourseOfferingRepository offeringRepo;
    private final CourseObjectiveRepository courseObjectiveRepo;
    private final AssessmentRepository assessmentRepo;
    private final ObjectiveIndicatorMatrixRepository oiMatrixRepo;
    private final StudentCourseRepository studentCourseRepo;
    private final StudentGradeRepository gradeRepo;
    private final GradeWeightConfigRepository weightConfigRepo;
    private final CourseResourceRepository resourceRepo;

    public DataInitializer(RoleRepository roleRepo,
                           UserRepository userRepo,
                           PasswordEncoder passwordEncoder,
                           ProgramRepository programRepo,
                           EducationalObjectiveRepository objectiveRepo,
                           GraduationRequirementRepository requirementRepo,
                           IndicatorPointRepository indicatorRepo,
                           ObjectiveRequirementMatrixRepository orMatrixRepo,
                           CourseRepository courseRepo,
                           CourseRequirementMatrixRepository crMatrixRepo,
                           CourseOfferingRepository offeringRepo,
                           CourseObjectiveRepository courseObjectiveRepo,
                           AssessmentRepository assessmentRepo,
                           ObjectiveIndicatorMatrixRepository oiMatrixRepo,
                           StudentCourseRepository studentCourseRepo,
                           StudentGradeRepository gradeRepo,
                           GradeWeightConfigRepository weightConfigRepo,
                           CourseResourceRepository resourceRepo) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.programRepo = programRepo;
        this.objectiveRepo = objectiveRepo;
        this.requirementRepo = requirementRepo;
        this.indicatorRepo = indicatorRepo;
        this.orMatrixRepo = orMatrixRepo;
        this.courseRepo = courseRepo;
        this.crMatrixRepo = crMatrixRepo;
        this.offeringRepo = offeringRepo;
        this.courseObjectiveRepo = courseObjectiveRepo;
        this.assessmentRepo = assessmentRepo;
        this.oiMatrixRepo = oiMatrixRepo;
        this.studentCourseRepo = studentCourseRepo;
        this.gradeRepo = gradeRepo;
        this.weightConfigRepo = weightConfigRepo;
        this.resourceRepo = resourceRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 检测旧数据：毕业要求 < 12 条说明是旧版精简数据，全部清除重建
        long grCount = requirementRepo.count();
        if (grCount > 0 && grCount < 12) {
            log.info("检测到旧版测试数据 ({} 条毕业要求)，清除并重新初始化全面数据...", grCount);
            clearAllData();
        }

        // 修复：补填课程目标编号（防止之前已初始化但无 code 的数据）
        fixEmptyObjectiveCodes();

        // 如果数据已完整，跳过
        if (grCount >= 12) {
            log.info("全面测试数据已存在 ({} 条毕业要求)，跳过初始化", grCount);
            return;
        }

        log.info("===========================================================");
        log.info("      开始初始化答辩演示用全面测试数据");
        log.info("===========================================================");

        // ============ 第1步：角色 ============
        initRoles();

        // ============ 第2步：用户 ============
        User admin = initAdmin();
        List<User> teachers = initTeachers();
        List<User> csStudents = initStudents(1, 10, "计算机科学与技术");   // student01~10
        List<User> seStudents = initStudents(11, 15, "软件工程");          // student11~15
        List<User> allStudents = new ArrayList<>();
        allStudents.addAll(csStudents);
        allStudents.addAll(seStudents);

        // ============ 第3步：培养方案 ============
        Program csProgram = initProgram("计算机科学与技术", "2024");
        Program seProgram = initProgram("软件工程", "2024");

        // ============ 第4步：培养目标 ============
        List<EducationalObjective> csObjectives = initObjectives(csProgram);
        List<EducationalObjective> seObjectives = initObjectives(seProgram);

        // ============ 第5步：毕业要求 (12条标准) ============
        List<GraduationRequirement> csRequirements = initGraduationRequirements(csProgram);
        List<GraduationRequirement> seRequirements = initGraduationRequirements(seProgram);

        // ============ 第6步：指标点 (每条GR 3个) ============
        List<IndicatorPoint> csIndicators = initIndicatorPoints(csRequirements);
        List<IndicatorPoint> seIndicators = initIndicatorPoints(seRequirements);

        // ============ 第7步：目标-要求支撑矩阵 ============
        initObjectiveRequirementMatrix(csObjectives, csRequirements);
        initObjectiveRequirementMatrix(seObjectives, seRequirements);

        // ============ 第8步：课程 (8门) ============
        List<Course> csCourses = initCSCourses(csProgram);
        List<Course> seCourses = initSECourses(seProgram);
        List<Course> allCourses = new ArrayList<>();
        allCourses.addAll(csCourses);
        allCourses.addAll(seCourses);

        // ============ 第9步：课程-指标点支撑矩阵 ============
        initCourseRequirementMatrix(csCourses, csIndicators);
        initCourseRequirementMatrix(seCourses, seIndicators);

        // ============ 第10步：开课记录 ============
        // 第1学期开课
        List<CourseOffering> sem1Offerings = new ArrayList<>();
        sem1Offerings.add(initOffering(csCourses.get(0), teachers.get(0), "2025-2026", "第1学期")); // CS101-张
        sem1Offerings.add(initOffering(csCourses.get(1), teachers.get(1), "2025-2026", "第1学期")); // CS102-李
        sem1Offerings.add(initOffering(seCourses.get(0), teachers.get(2), "2025-2026", "第1学期")); // SE101-王
        sem1Offerings.add(initOffering(seCourses.get(1), teachers.get(3), "2025-2026", "第1学期")); // SE102-赵
        // 第2学期开课
        List<CourseOffering> sem2Offerings = new ArrayList<>();
        sem2Offerings.add(initOffering(csCourses.get(2), teachers.get(0), "2025-2026", "第2学期")); // CS103-张
        sem2Offerings.add(initOffering(csCourses.get(3), teachers.get(1), "2025-2026", "第2学期")); // CS104-李
        sem2Offerings.add(initOffering(seCourses.get(2), teachers.get(2), "2025-2026", "第2学期")); // SE103-王
        sem2Offerings.add(initOffering(seCourses.get(3), teachers.get(3), "2025-2026", "第2学期")); // SE104-赵

        List<CourseOffering> allOfferings = new ArrayList<>();
        allOfferings.addAll(sem1Offerings);
        allOfferings.addAll(sem2Offerings);

        // ============ 第11步：学生选课 ============
        for (CourseOffering offering : sem1Offerings) {
            if (offering.getCourseId().equals(csCourses.get(0).getId())
                    || offering.getCourseId().equals(csCourses.get(1).getId())) {
                initStudentCourses(offering, csStudents);
            } else {
                initStudentCourses(offering, seStudents);
            }
        }
        for (CourseOffering offering : sem2Offerings) {
            if (offering.getCourseId().equals(csCourses.get(2).getId())
                    || offering.getCourseId().equals(csCourses.get(3).getId())) {
                initStudentCourses(offering, csStudents);
            } else {
                initStudentCourses(offering, seStudents);
            }
        }

        // ============ 第12步：课程目标 + 考核 + 成绩 + 权重 + 矩阵 + 资源 ============
        for (CourseOffering offering : allOfferings) {
            List<CourseObjective> objList = initCourseObjectives(offering);
            initAssessments(offering, objList);
            initGradeWeightConfig(offering);
            initCourseResources(offering);
            // 判断该开课对应的学生列表
            List<User> studentList;
            if (offering.getCourseId().equals(csCourses.get(0).getId())
                    || offering.getCourseId().equals(csCourses.get(1).getId())
                    || offering.getCourseId().equals(csCourses.get(2).getId())
                    || offering.getCourseId().equals(csCourses.get(3).getId())) {
                studentList = csStudents;
            } else {
                studentList = seStudents;
            }
            initStudentGrades(offering, studentList);
            // 课程目标-指标点矩阵：需要判断课程属于哪个专业
            List<IndicatorPoint> indicatorList;
            if (offering.getCourseId().equals(csCourses.get(0).getId())
                    || offering.getCourseId().equals(csCourses.get(1).getId())
                    || offering.getCourseId().equals(csCourses.get(2).getId())
                    || offering.getCourseId().equals(csCourses.get(3).getId())) {
                indicatorList = csIndicators;
            } else {
                indicatorList = seIndicators;
            }
            initObjectiveIndicatorMatrix(objList, indicatorList, offering.getCourseId());
        }

        log.info("===========================================================");
        log.info("      答辩演示测试数据初始化完成！");
        log.info("      管理员: admin/123456");
        log.info("      教师: teacher01~04/123456");
        log.info("      学生: student01~15/123456");
        log.info("===========================================================");
    }

    // ==================== 修复空编号 ====================

    /**
     * 补填课程目标中为空的 code 字段（按每门开课自动编号 CO1, CO2, ...）
     */
    private void fixEmptyObjectiveCodes() {
        List<CourseObjective> all = courseObjectiveRepo.findAll();
        // 按 offeringId 分组
        Map<Long, List<CourseObjective>> grouped = new java.util.LinkedHashMap<>();
        for (CourseObjective obj : all) {
            grouped.computeIfAbsent(obj.getOfferingId(), k -> new ArrayList<>()).add(obj);
        }

        int fixed = 0;
        for (Map.Entry<Long, List<CourseObjective>> entry : grouped.entrySet()) {
            List<CourseObjective> list = entry.getValue();
            // 按 ID 排序确保编号稳定
            list.sort(Comparator.comparing(CourseObjective::getId));

            // 检查是否有空 code 的
            boolean hasEmpty = false;
            for (CourseObjective obj : list) {
                if (obj.getCode() == null || obj.getCode().isBlank()) {
                    hasEmpty = true;
                    break;
                }
            }
            if (!hasEmpty) continue;

            // 重新编号
            for (int i = 0; i < list.size(); i++) {
                CourseObjective obj = list.get(i);
                String newCode = "CO" + (i + 1);
                if (!newCode.equals(obj.getCode())) {
                    obj.setCode(newCode);
                    courseObjectiveRepo.save(obj);
                    fixed++;
                }
            }
        }
        if (fixed > 0) {
            log.info("  ✓ 补填课程目标编号: {} 条", fixed);
        }
    }

    // ==================== 清除旧数据 ====================
    private void clearAllData() {
        log.info("清除所有旧数据（保留 admin 账号）...");
        // 按依赖顺序从叶子到根删除
        resourceRepo.deleteAll();
        gradeRepo.deleteAll();
        weightConfigRepo.deleteAll();
        studentCourseRepo.deleteAll();
        assessmentRepo.deleteAll();
        oiMatrixRepo.deleteAll();
        crMatrixRepo.deleteAll();
        courseObjectiveRepo.deleteAll();
        offeringRepo.deleteAll();
        courseRepo.deleteAll();
        orMatrixRepo.deleteAll();
        indicatorRepo.deleteAll();
        requirementRepo.deleteAll();
        objectiveRepo.deleteAll();
        programRepo.deleteAll();
        // 删除非 admin 用户
        List<User> allUsers = userRepo.findAll();
        for (User u : allUsers) {
            if (!"admin".equals(u.getUsername())) {
                userRepo.delete(u);
            }
        }
        log.info("旧数据清除完毕（保留 admin）");
    }

    // ==================== 1. 角色 ====================
    private void initRoles() {
        for (String name : new String[]{"admin", "teacher", "student"}) {
            if (roleRepo.findByRoleName(name).isEmpty()) {
                Role role = new Role();
                role.setRoleName(name);
                roleRepo.save(role);
                log.info("  ✓ 角色: {}", name);
            }
        }
    }

    // ==================== 2. 管理员 ====================
    private User initAdmin() {
        return userRepo.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setPassword(passwordEncoder.encode("123456"));
            u.setName("系统管理员");
            u.setRole("admin");
            u.setStatus(1);
            User saved = userRepo.save(u);
            log.info("  ✓ 管理员: admin / 123456");
            return saved;
        });
    }

    // ==================== 3. 教师 (4人) ====================
    private List<User> initTeachers() {
        List<User> teachers = new ArrayList<>();
        String[][] data = {
                {"teacher01", "张教授", "13800000001", "zhang@university.edu", "计算机学院", "计算机科学与技术"},
                {"teacher02", "李教授", "13800000002", "li@university.edu",   "计算机学院", "计算机科学与技术"},
                {"teacher03", "王教授", "13800000003", "wang@university.edu", "软件学院",   "软件工程"},
                {"teacher04", "赵教授", "13800000004", "zhao@university.edu", "软件学院",   "软件工程"},
        };
        for (String[] d : data) {
            User t = userRepo.findByUsername(d[0]).orElseGet(() -> {
                User u = new User();
                u.setUsername(d[0]);
                u.setPassword(passwordEncoder.encode("123456"));
                u.setName(d[1]);
                u.setRole("teacher");
                u.setPhone(d[2]);
                u.setEmail(d[3]);
                u.setCollege(d[4]);
                u.setMajor(d[5]);
                u.setStatus(1);
                return userRepo.save(u);
            });
            teachers.add(t);
        }
        log.info("  ✓ 教师: teacher01~04 / 123456");
        return teachers;
    }

    // ==================== 4. 学生 (15人) ====================
    private List<User> initStudents(int from, int to, String major) {
        List<User> students = new ArrayList<>();
        String[] names = {
                "赵一", "钱二", "孙三", "李四", "周五",
                "吴六", "郑七", "王八", "冯九", "陈十",
                "褚十一", "卫十二", "蒋十三", "沈十四", "韩十五"
        };
        String college = major.contains("软件") ? "软件学院" : "计算机学院";
        String grade = "2024级";
        for (int i = from; i <= to; i++) {
            final int idx = i; // effectively final for lambda
            String username = String.format("student%02d", i);
            String name = (idx - 1) < names.length ? names[idx - 1] : "学生" + idx;
            String className = (idx % 3 + 1) + "班";

            User s = userRepo.findByUsername(username).orElseGet(() -> {
                User u = new User();
                u.setUsername(username);
                u.setPassword(passwordEncoder.encode("123456"));
                u.setName(name);
                u.setRole("student");
                u.setCollege(college);
                u.setMajor(major);
                u.setGrade(grade);
                u.setClassName(className);
                u.setStatus(1);
                return userRepo.save(u);
            });
            students.add(s);
        }
        log.info("  ✓ {} 学生: student{:02d}~{:02d} / 123456 ({})", students.size(), from, to, major);
        return students;
    }

    // ==================== 5. 培养方案 ====================
    private Program initProgram(String majorName, String version) {
        return programRepo.findByMajorName(majorName).orElseGet(() -> {
            Program p = new Program();
            p.setMajorName(majorName);
            p.setVersion(version);
            p.setStatus("published");
            Program saved = programRepo.save(p);
            log.info("  ✓ 培养方案: {} {}版 (已发布, id={})", majorName, version, saved.getId());
            return saved;
        });
    }

    // ==================== 6. 培养目标 (每专业3条) ====================
    private List<EducationalObjective> initObjectives(Program program) {
        List<EducationalObjective> existing = objectiveRepo.findByProgramIdOrderBySortOrderAsc(program.getId());
        if (!existing.isEmpty()) return existing;

        String major = program.getMajorName();
        List<EducationalObjective> list = new ArrayList<>();
        String[][] data;
        if (major.contains("计算机科学")) {
            data = new String[][]{
                    {"培养具备扎实计算机科学理论基础和工程实践能力的专业人才，能够运用数学、自然科学和计算机专业知识解决复杂工程问题", "1"},
                    {"培养能够从事计算机系统设计、开发、应用和维护的高素质技术人才，具备软件架构设计和系统集成能力", "2"},
                    {"培养具备创新精神、团队协作能力和国际视野的复合型人才，能够在多学科团队中发挥骨干作用", "3"},
            };
        } else {
            data = new String[][]{
                    {"培养掌握软件工程基本理论和方法的专业人才，具备软件需求分析、设计、开发、测试和维护的全流程能力", "1"},
                    {"培养具备大型软件系统分析和设计能力的高级工程技术人才，能够运用工程化方法管理软件项目", "2"},
                    {"培养具备良好的项目管理能力和团队协作精神的软件工程师，能够在敏捷开发环境中高效工作", "3"},
            };
        }
        for (String[] d : data) {
            EducationalObjective obj = new EducationalObjective();
            obj.setProgramId(program.getId());
            obj.setDescription(d[0]);
            obj.setSortOrder(Integer.parseInt(d[1]));
            list.add(objectiveRepo.save(obj));
        }
        log.info("  ✓ {} 培养目标: 3条", major);
        return list;
    }

    // ==================== 7. 毕业要求 (12条标准) ====================
    private List<GraduationRequirement> initGraduationRequirements(Program program) {
        List<GraduationRequirement> existing = requirementRepo.findByProgramId(program.getId());
        if (!existing.isEmpty()) return existing;

        String[][] data = {
                {"1", "工程知识", "能够将数学、自然科学、工程基础和专业知识用于解决计算机领域的复杂工程问题"},
                {"2", "问题分析", "能够应用数学、自然科学和工程科学的基本原理，识别、表达并通过文献研究分析复杂工程问题，以获得有效结论"},
                {"3", "设计/开发解决方案", "能够设计针对复杂工程问题的解决方案，设计满足特定需求的系统、单元（组件）或工艺流程，并能够在设计环节中体现创新意识，考虑社会、健康、安全、法律、文化以及环境等因素"},
                {"4", "研究", "能够基于科学原理并采用科学方法对复杂工程问题进行研究，包括设计实验、分析与解释数据，并通过信息综合得到合理有效的结论"},
                {"5", "使用现代工具", "能够针对复杂工程问题，开发、选择与使用恰当的技术、资源、现代工程工具和信息技术工具，包括对复杂工程问题的预测与模拟，并能够理解其局限性"},
                {"6", "工程与社会", "能够基于工程相关背景知识进行合理分析，评价专业工程实践和复杂工程问题解决方案对社会、健康、安全、法律以及文化的影响，并理解应承担的责任"},
                {"7", "环境和可持续发展", "能够理解和评价针对复杂工程问题的工程实践对环境、社会可持续发展的影响"},
                {"8", "职业规范", "具有人文社会科学素养、社会责任感，能够在工程实践中理解并遵守工程职业道德和规范，履行责任"},
                {"9", "个人和团队", "能够在多学科背景下的团队中承担个体、团队成员以及负责人的角色"},
                {"10", "沟通", "能够就复杂工程问题与业界同行及社会公众进行有效沟通和交流，包括撰写报告和设计文稿、陈述发言、清晰表达或回应指令。并具备一定的国际视野，能够在跨文化背景下进行沟通和交流"},
                {"11", "项目管理", "理解并掌握工程管理原理与经济决策方法，并能在多学科环境中应用"},
                {"12", "终身学习", "具有自主学习和终身学习的意识，有不断学习和适应发展的能力"},
        };

        List<GraduationRequirement> list = new ArrayList<>();
        for (String[] d : data) {
            GraduationRequirement gr = new GraduationRequirement();
            gr.setProgramId(program.getId());
            gr.setCode(d[0]);
            gr.setDescription(d[1] + "：" + d[2]);
            list.add(requirementRepo.save(gr));
        }
        log.info("  ✓ {} 毕业要求: 12条 (GR1~GR12)", program.getMajorName());
        return list;
    }

    // ==================== 8. 指标点 (每条GR 3个) ====================
    private List<IndicatorPoint> initIndicatorPoints(List<GraduationRequirement> requirements) {
        List<Long> reqIds = new ArrayList<>();
        for (GraduationRequirement r : requirements) reqIds.add(r.getId());
        List<IndicatorPoint> existing = indicatorRepo.findByRequirementIdIn(reqIds);
        if (!existing.isEmpty() && existing.size() >= requirements.size() * 3) {
            return existing;
        }

        // 每条毕业要求的3个指标点描述
        String[][][] ipData = {
                // GR1 工程知识
                {{"1.1", "掌握数学和自然科学基础知识，具备运用数学方法进行抽象建模和逻辑推理的能力", "0.030"},
                 {"1.2", "掌握计算机科学与技术核心专业知识，包括程序设计、数据结构、算法分析与设计等", "0.030"},
                 {"1.3", "能够将工程基础知识应用于计算机系统分析与设计，解决实际工程问题", "0.030"}},
                // GR2 问题分析
                {{"2.1", "能够运用计算机科学基本原理，识别和表述复杂工程问题的关键要素和约束条件", "0.028"},
                 {"2.2", "能够通过文献检索、资料查询等方法，分析复杂工程问题的国内外研究现状和发展趋势", "0.028"},
                 {"2.3", "能够综合运用所学知识，对复杂工程问题进行分析、建模，并获得有效结论", "0.028"}},
                // GR3 设计/开发
                {{"3.1", "能够根据用户需求，设计满足特定功能和非功能需求的软件系统方案", "0.028"},
                 {"3.2", "能够在系统设计中综合考虑社会、健康、安全、法律、文化及环境等非技术因素", "0.028"},
                 {"3.3", "能够在设计过程中体现创新意识，提出改进或替代方案", "0.028"}},
                // GR4 研究
                {{"4.1", "能够基于计算机科学原理，针对特定工程问题设计合理的实验方案", "0.025"},
                 {"4.2", "能够运用现代工具和方法进行数据采集、处理和分析，正确解释实验数据", "0.025"},
                 {"4.3", "能够综合实验数据和分析结果，得出合理有效的结论并撰写研究报告", "0.025"}},
                // GR5 使用现代工具
                {{"5.1", "了解计算机领域常用的开发工具、平台和框架，理解其原理和适用场景", "0.025"},
                 {"5.2", "能够针对具体工程问题，选择并熟练使用合适的开发工具和平台", "0.025"},
                 {"5.3", "能够利用现代工具进行复杂工程问题的建模、仿真和预测，并理解工具的局限性", "0.025"}},
                // GR6 工程与社会
                {{"6.1", "了解计算机技术与应用相关的社会、健康、安全、法律及文化等方面的基本知识", "0.025"},
                 {"6.2", "能够分析和评价计算机工程实践对社会的影响，理解工程技术的社会价值", "0.025"},
                 {"6.3", "理解软件工程师在信息安全、隐私保护等方面的社会责任和职业操守", "0.025"}},
                // GR7 环境与可持续发展
                {{"7.1", "了解环境保护和可持续发展的基本理念和相关法律法规", "0.022"},
                 {"7.2", "能够理解和评价计算机工程实践对环境和可持续发展的影响", "0.022"},
                 {"7.3", "了解绿色计算、节能减排等信息技术领域的可持续发展实践", "0.022"}},
                // GR8 职业规范
                {{"8.1", "具有良好的人文社会科学素养和正确的价值观，理解个人与社会的关系", "0.028"},
                 {"8.2", "理解计算机工程师的职业性质和责任，能够在工程实践中遵守职业道德规范", "0.028"},
                 {"8.3", "了解软件工程领域的知识产权、专利等法律知识，尊重他人劳动成果", "0.028"}},
                // GR9 个人和团队
                {{"9.1", "能够在多学科团队中承担个体角色，完成分配的任务并按时交付", "0.028"},
                 {"9.2", "具备良好的团队协作能力，能够与其他成员有效沟通和协作", "0.028"},
                 {"9.3", "具备一定的组织协调能力，能够在适当情况下担任团队负责人的角色", "0.028"}},
                // GR10 沟通
                {{"10.1", "能够撰写规范的技术文档、实验报告和项目总结，表达清晰、逻辑严谨", "0.028"},
                 {"10.2", "能够进行有效的口头陈述和演示，清晰表达技术方案和设计思路", "0.028"},
                 {"10.3", "具备一定的英语应用能力，能够阅读英文技术文献并进行基本的国际交流", "0.028"}},
                // GR11 项目管理
                {{"11.1", "理解软件项目管理的基本原理和方法，包括项目计划、进度控制和质量管理", "0.022"},
                 {"11.2", "掌握基本的工程经济决策方法，能够进行成本效益分析", "0.022"},
                 {"11.3", "能够在多学科环境中应用项目管理知识，有效组织和管理工程项目", "0.022"}},
                // GR12 终身学习
                {{"12.1", "具有自主学习的意识，能够认识到持续学习对于职业发展的重要性", "0.028"},
                 {"12.2", "具备信息获取能力，能够利用多种渠道跟踪计算机领域的最新技术发展", "0.028"},
                 {"12.3", "具有良好的学习习惯和方法，能够不断适应技术变革和职业发展需求", "0.028"}},
        };

        List<IndicatorPoint> all = new ArrayList<>();
        for (int i = 0; i < requirements.size() && i < ipData.length; i++) {
            for (String[] d : ipData[i]) {
                IndicatorPoint ip = new IndicatorPoint();
                ip.setRequirementId(requirements.get(i).getId());
                ip.setCode(d[0]);
                ip.setDescription(d[1]);
                ip.setWeight(new BigDecimal(d[2]));
                ip.setPassScore(new BigDecimal("0.60"));
                all.add(indicatorRepo.save(ip));
            }
        }
        log.info("  ✓ 指标点: {} 个 (每条毕业要求3个)", all.size());
        return all;
    }

    // ==================== 9. 目标-要求支撑矩阵 ====================
    private void initObjectiveRequirementMatrix(List<EducationalObjective> objectives,
                                                 List<GraduationRequirement> requirements) {
        // 检查这些 objectives 是否已有矩阵数据
        List<Long> objIds = new ArrayList<>();
        for (EducationalObjective obj : objectives) objIds.add(obj.getId());
        if (!orMatrixRepo.findByObjectiveIdIn(objIds).isEmpty()) return;

        // 目标1 (基础能力) → GR1, GR2, GR5 (H), GR8, GR12 (M)
        // 目标2 (工程实践) → GR3, GR4, GR9, GR10, GR11 (H), GR6, GR7 (M)
        // 目标3 (综合素质) → GR6, GR7, GR8, GR9, GR10, GR12 (H)

        // 简化：建立全面支撑关系
        String[][] matrix = {
                // obj_idx, req_idx, level
                {"0", "0", "H"}, {"0", "1", "H"}, {"0", "3", "H"}, {"0", "4", "H"},
                {"0", "5", "M"}, {"0", "7", "M"}, {"0", "11", "M"},
                {"1", "2", "H"}, {"1", "3", "H"}, {"1", "5", "M"}, {"1", "6", "M"},
                {"1", "8", "H"}, {"1", "9", "H"}, {"1", "10", "H"},
                {"2", "5", "H"}, {"2", "6", "H"}, {"2", "7", "H"},
                {"2", "8", "H"}, {"2", "9", "H"}, {"2", "10", "M"}, {"2", "11", "H"},
        };
        int count = 0;
        for (String[] m : matrix) {
            ObjectiveRequirementMatrix orm = new ObjectiveRequirementMatrix();
            orm.setObjectiveId(objectives.get(Integer.parseInt(m[0])).getId());
            orm.setRequirementId(requirements.get(Integer.parseInt(m[1])).getId());
            orm.setSupportLevel(m[2]);
            orMatrixRepo.save(orm);
            count++;
        }
        log.info("  ✓ 培养目标-毕业要求支撑矩阵: {} 条", count);
    }

    // ==================== 10. 课程 ====================
    // CS 课程 (4门)
    private List<Course> initCSCourses(Program program) {
        List<Course> list = new ArrayList<>();
        String[][] data = {
                {"CS101", "数据结构",    "4.0", "64", "48", "16", "2025-1", "专业核心", "true"},
                {"CS102", "操作系统",    "4.0", "64", "52", "12", "2025-1", "专业核心", "true"},
                {"CS103", "计算机网络",  "3.0", "48", "36", "12", "2025-2", "专业核心", "true"},
                {"CS104", "数据库原理",  "3.0", "48", "36", "12", "2025-2", "专业必修", "true"},
        };
        for (String[] d : data) {
            list.add(createCourse(d[0], d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], program.getId()));
        }
        log.info("  ✓ 计算机科学课程: {} 门", list.size());
        return list;
    }

    // SE 课程 (4门)
    private List<Course> initSECourses(Program program) {
        List<Course> list = new ArrayList<>();
        String[][] data = {
                {"SE101", "面向对象程序设计", "4.0", "64", "40", "24", "2025-1", "专业核心", "true"},
                {"SE102", "软件测试",         "3.0", "48", "32", "16", "2025-1", "专业必修", "true"},
                {"SE103", "Web前端开发",      "3.0", "48", "24", "24", "2025-2", "专业选修", "false"},
                {"SE104", "软件项目管理",     "3.0", "48", "40", "8",  "2025-2", "专业必修", "true"},
        };
        for (String[] d : data) {
            list.add(createCourse(d[0], d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], program.getId()));
        }
        log.info("  ✓ 软件工程课程: {} 门", list.size());
        return list;
    }

    private Course createCourse(String code, String name, String credits, String totalHours,
                                 String theoryHours, String labHours, String semester,
                                 String courseType, String isRequired, Long programId) {
        return courseRepo.findByCode(code).orElseGet(() -> {
            Course c = new Course();
            c.setCode(code);
            c.setName(name);
            c.setCredits(new BigDecimal(credits));
            c.setTotalHours(Integer.parseInt(totalHours));
            c.setTheoryHours(Integer.parseInt(theoryHours));
            c.setLabHours(Integer.parseInt(labHours));
            c.setSemester(semester);
            c.setCourseType(courseType);
            c.setIsRequired(Boolean.parseBoolean(isRequired));
            c.setProgramId(programId);
            return courseRepo.save(c);
        });
    }

    // ==================== 11. 课程-指标点支撑矩阵 ====================
    private void initCourseRequirementMatrix(List<Course> courses, List<IndicatorPoint> indicators) {
        // 检查这些 courses 是否已有矩阵数据
        boolean hasData = false;
        for (Course c : courses) {
            if (!crMatrixRepo.findByCourseId(c.getId()).isEmpty()) { hasData = true; break; }
        }
        if (hasData) return;

        // 每门课程支撑若干指标点（根据课程内容匹配）
        // CS101 数据结构: 1.1(H), 1.2(H), 2.1(M), 3.1(M), 4.1(M)
        // CS102 操作系统: 1.2(H), 2.3(M), 3.1(H), 5.2(M)
        // CS103 计算机网络: 1.3(H), 2.2(M), 3.1(M), 5.2(H), 6.2(M)
        // CS104 数据库原理: 1.2(H), 2.1(M), 3.1(H), 4.2(M), 11.1(M)
        // SE101 OOP: 1.2(H), 1.3(H), 3.1(M), 5.1(M), 9.1(M)
        // SE102 软件测试: 2.1(H), 4.1(H), 5.2(M), 10.1(M)
        // SE103 Web前端: 3.1(H), 5.1(H), 5.2(M), 9.1(M)
        // SE104 软件项目管理: 3.2(M), 8.1(M), 9.1(H), 10.1(H), 11.1(H)

        // indicator 索引映射: GR_i * 3 + j = GR i 的第 j 个指标点
        // GR1: idx 0,1,2; GR2: 3,4,5; GR3: 6,7,8; ... GR12: 33,34,35
        Object[][][] courseMatrixMap = {
                // CS101 (courses[0]) → 数据结构相关指标点
                {{0, "H"}, {1, "H"}, {3, "M"}, {6, "M"}, {12, "M"}},
                // CS102 (courses[1]) → 操作系统相关
                {{1, "H"}, {5, "M"}, {6, "H"}, {16, "M"}},
                // CS103 (courses[2]) → 计算机网络相关
                {{2, "H"}, {4, "M"}, {7, "M"}, {16, "H"}, {18, "M"}},
                // CS104 (courses[3]) → 数据库相关
                {{1, "H"}, {3, "M"}, {7, "H"}, {13, "M"}, {33, "M"}},
                // SE101 (courses[4]) → OOP
                {{1, "H"}, {2, "H"}, {7, "M"}, {15, "M"}, {27, "M"}},
                // SE102 (courses[5]) → 软件测试
                {{3, "H"}, {12, "H"}, {16, "M"}, {30, "M"}},
                // SE103 (courses[6]) → Web前端
                {{7, "H"}, {15, "H"}, {16, "M"}, {27, "M"}},
                // SE104 (courses[7]) → 项目管理
                {{8, "M"}, {23, "M"}, {27, "H"}, {30, "H"}, {33, "H"}},
        };

        int count = 0;
        for (int c = 0; c < courses.size() && c < courseMatrixMap.length; c++) {
            for (Object[] m : courseMatrixMap[c]) {
                int idx = (int) m[0];
                if (idx < indicators.size()) {
                    CourseRequirementMatrix crm = new CourseRequirementMatrix();
                    crm.setCourseId(courses.get(c).getId());
                    crm.setIndicatorId(indicators.get(idx).getId());
                    crm.setSupportLevel((String) m[1]);
                    crMatrixRepo.save(crm);
                    count++;
                }
            }
        }
        log.info("  ✓ 课程-指标点支撑矩阵: {} 条", count);
    }

    // ==================== 12. 开课记录 ====================
    private CourseOffering initOffering(Course course, User teacher,
                                         String academicYear, String semester) {
        boolean exists = offeringRepo.existsByCourseIdAndTeacherIdAndAcademicYearAndSemester(
                course.getId(), teacher.getId(), academicYear, semester);
        if (exists) {
            // 查找已存在的记录
            List<CourseOffering> list = offeringRepo.findByTeacherId(teacher.getId());
            for (CourseOffering o : list) {
                if (o.getCourseId().equals(course.getId())
                        && o.getAcademicYear().equals(academicYear)
                        && o.getSemester().equals(semester)) {
                    return o;
                }
            }
        }

        CourseOffering o = new CourseOffering();
        o.setCourseId(course.getId());
        o.setTeacherId(teacher.getId());
        o.setAcademicYear(academicYear);
        o.setSemester(semester);
        CourseOffering saved = offeringRepo.save(o);
        log.info("  ✓ 开课: {} {} {} ({}老师)", course.getCode(), academicYear, semester, teacher.getName());
        return saved;
    }

    // ==================== 13. 学生选课 ====================
    private void initStudentCourses(CourseOffering offering, List<User> students) {
        if (studentCourseRepo.existsByOfferingId(offering.getId())) return;
        for (User s : students) {
            StudentCourse sc = new StudentCourse();
            sc.setOfferingId(offering.getId());
            sc.setStudentId(s.getId());
            studentCourseRepo.save(sc);
        }
    }

    // ==================== 14. 课程目标 (每开课3~4个) ====================
    private List<CourseObjective> initCourseObjectives(CourseOffering offering) {
        List<CourseObjective> existing = courseObjectiveRepo.findByOfferingId(offering.getId());
        if (!existing.isEmpty()) return existing;

        // 根据课程ID（通过course name判断）返回不同目标
        Course course = courseRepo.findById(offering.getCourseId()).orElse(null);
        String name = course != null ? course.getName() : "";

        String[][] objData = getObjectiveDataForCourse(name);
        List<CourseObjective> list = new ArrayList<>();
        for (int i = 0; i < objData.length; i++) {
            String[] d = objData[i];
            CourseObjective obj = new CourseObjective();
            obj.setOfferingId(offering.getId());
            obj.setCode("CO" + (i + 1));
            obj.setDescription(d[0]);
            obj.setWeight(new BigDecimal(d[1]));
            list.add(courseObjectiveRepo.save(obj));
        }
        return list;
    }

    private String[][] getObjectiveDataForCourse(String courseName) {
        switch (courseName) {
            case "数据结构":
                return new String[][]{
                        {"掌握线性表、栈、队列、树和图等基本数据结构的定义、存储结构及基本操作", "0.35"},
                        {"理解算法时间复杂度和空间复杂度的分析方法，能够对常见算法进行效率评估与比较", "0.30"},
                        {"具备根据实际问题场景选择合适数据结构并设计高效算法的能力", "0.35"},
                };
            case "操作系统":
                return new String[][]{
                        {"理解操作系统的基本概念、功能和体系结构，掌握进程管理和进程调度算法", "0.30"},
                        {"掌握内存管理机制（分区、分页、分段、虚拟内存）和文件系统的基本原理", "0.30"},
                        {"理解并发控制、死锁处理和I/O管理的原理，能够分析和解决实际系统中的相关问题", "0.40"},
                };
            case "计算机网络":
                return new String[][]{
                        {"理解计算机网络体系结构（OSI/TCP-IP模型），掌握各层协议的基本原理", "0.30"},
                        {"掌握IP编址、路由选择、拥塞控制等网络层核心技术", "0.30"},
                        {"理解传输层协议（TCP/UDP）和应用层常用协议（HTTP/DNS/SMTP）的工作机制", "0.40"},
                };
            case "数据库原理":
                return new String[][]{
                        {"掌握关系数据库的基本概念（关系模型、关系代数），能够进行数据库逻辑设计", "0.35"},
                        {"熟练掌握SQL语言，能够编写复杂查询、视图、存储过程和触发器", "0.35"},
                        {"理解事务管理、并发控制和数据库恢复技术，具备数据库性能调优意识", "0.30"},
                };
            case "面向对象程序设计":
                return new String[][]{
                        {"掌握面向对象编程的核心概念（封装、继承、多态），能够设计合理的类层次结构", "0.35"},
                        {"理解设计原则（SOLID）和常用设计模式，能够在项目中应用", "0.30"},
                        {"熟练使用Java/C++等面向对象语言进行应用程序开发，具备良好的编码规范意识", "0.35"},
                };
            case "软件测试":
                return new String[][]{
                        {"理解软件测试的基本概念和方法（黑盒/白盒测试），掌握测试用例设计技术", "0.30"},
                        {"掌握单元测试、集成测试、系统测试等不同层次的测试策略和实施方法", "0.30"},
                        {"了解自动化测试工具和框架（JUnit/Selenium等），能够编写自动化测试脚本", "0.40"},
                };
            case "Web前端开发":
                return new String[][]{
                        {"掌握HTML5语义化标签和CSS3布局技术，能够构建响应式网页", "0.30"},
                        {"熟练掌握JavaScript核心语法和DOM操作，理解ES6+新特性", "0.30"},
                        {"了解Vue/React等主流前端框架的开发模式和组件化开发思想", "0.40"},
                };
            case "软件项目管理":
                return new String[][]{
                        {"理解软件项目管理的基本框架（范围管理、时间管理、成本管理），掌握项目计划制定方法", "0.30"},
                        {"了解敏捷开发方法论（Scrum/Kanban），能够在实际项目中运用敏捷实践", "0.30"},
                        {"掌握项目进度跟踪、风险管理和质量保证的基本方法，具备团队协作管理能力", "0.40"},
                };
            default:
                return new String[][]{
                        {"掌握课程核心概念和基本原理", "0.35"},
                        {"能够运用所学知识分析和解决实际问题", "0.35"},
                        {"具备实践应用能力和创新意识", "0.30"},
                };
        }
    }

    // ==================== 15. 考核环节 ====================
    private void initAssessments(CourseOffering offering, List<CourseObjective> objectives) {
        List<Assessment> existing = assessmentRepo.findByOfferingId(offering.getId());
        if (!existing.isEmpty()) return;

        Object[][] data = {
                {"平时作业", "0.20", 0},    // → CO1
                {"实验报告", "0.20", 0},    // → CO1
                {"期中考试", "0.30", 1},    // → CO2
                {"期末考试", "0.30", objectives.size() >= 3 ? 2 : 1}, // → CO3（或CO2）
        };
        for (Object[] d : data) {
            Assessment a = new Assessment();
            a.setOfferingId(offering.getId());
            a.setName((String) d[0]);
            a.setWeight(new BigDecimal((String) d[1]));
            a.setObjectiveId(objectives.get((int) d[2]).getId());
            assessmentRepo.save(a);
        }
    }

    // ==================== 16. 成绩权重配置 ====================
    private void initGradeWeightConfig(CourseOffering offering) {
        if (weightConfigRepo.findByOfferingId(offering.getId()).isPresent()) return;
        GradeWeightConfig cfg = new GradeWeightConfig();
        cfg.setOfferingId(offering.getId());
        cfg.setDailyWeight(new BigDecimal("0.2500"));
        cfg.setReportWeight(new BigDecimal("0.2500"));
        cfg.setMidtermWeight(new BigDecimal("0.2500"));
        cfg.setFinalWeight(new BigDecimal("0.2500"));
        weightConfigRepo.save(cfg);
    }

    // ==================== 17. 学生成绩 (四列) ====================
    private void initStudentGrades(CourseOffering offering, List<User> students) {
        List<StudentGrade> existing = gradeRepo.findByOfferingId(offering.getId());
        if (!existing.isEmpty()) return;

        Random rnd = new Random(42L); // 固定种子保证可重复
        int total = 0;
        for (User student : students) {
            // 根据学生序号生成不同水平的成绩
            int idx = Integer.parseInt(student.getUsername().replace("student", ""));
            int base;
            int spread;
            if (idx <= 3 || idx == 11) {           // 优秀学生 (85-98)
                base = 88; spread = 10;
            } else if (idx <= 7 || idx == 12 || idx == 13) { // 中等学生 (70-88)
                base = 75; spread = 14;
            } else {                                // 需要关注的学生 (55-75)
                base = 58; spread = 18;
            }

            int daily   = clamp(base + rnd.nextInt(spread) - spread/3);
            int report  = clamp(base + rnd.nextInt(spread) - spread/3);
            int midterm = clamp(base + rnd.nextInt(spread) - spread/3);
            int finalS  = clamp(base + rnd.nextInt(spread) - spread/3);

            StudentGrade sg = new StudentGrade();
            sg.setOfferingId(offering.getId());
            sg.setStudentId(student.getId());
            sg.setDailyScore(new BigDecimal(daily));
            sg.setReportScore(new BigDecimal(report));
            sg.setMidtermScore(new BigDecimal(midterm));
            sg.setFinalScore(new BigDecimal(finalS));

            // 加权总分（默认各0.25 = 简单平均）
            BigDecimal totalScore = new BigDecimal(daily + report + midterm + finalS)
                    .divide(new BigDecimal("4"), 2, RoundingMode.HALF_UP);
            sg.setTotalScore(totalScore);

            gradeRepo.save(sg);
            total++;
        }
        log.info("  ✓ 成绩: {} 条 (开课id={})", total, offering.getId());
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    // ==================== 18. 课程目标-指标点支撑矩阵 ====================
    private void initObjectiveIndicatorMatrix(List<CourseObjective> objectives,
                                               List<IndicatorPoint> indicators,
                                               Long courseId) {
        // 检查该课程的 objectives 是否已有矩阵数据
        List<Long> objIds = new ArrayList<>();
        for (CourseObjective obj : objectives) objIds.add(obj.getId());
        if (!oiMatrixRepo.findByObjectiveIdIn(objIds).isEmpty()) return;

        // 根据课程ID选择支撑的指标点
        Course course = courseRepo.findById(courseId).orElse(null);
        String code = course != null ? course.getCode() : "";

        // CO1 → 2个指标点(H,M), CO2 → 2个指标点(H,M), CO3 → 1~2个指标点(H,M)
        int[][] indicatorIndices = getIndicatorIndicesForCourse(code);

        int count = 0;
        for (int oi = 0; oi < objectives.size() && oi < indicatorIndices.length; oi++) {
            int[] idxs = indicatorIndices[oi];
            String[] levels = (oi == 0) ? new String[]{"H", "M"}
                    : (oi == 1) ? new String[]{"H", "M"}
                    : new String[]{"H", "M"};

            for (int j = 0; j < idxs.length && j < levels.length; j++) {
                int idx = idxs[j];
                if (idx < indicators.size()) {
                    ObjectiveIndicatorMatrix oim = new ObjectiveIndicatorMatrix();
                    oim.setObjectiveId(objectives.get(oi).getId());
                    oim.setIndicatorId(indicators.get(idx).getId());
                    oim.setSupportLevel(levels[j]);
                    oiMatrixRepo.save(oim);
                    count++;
                }
            }
        }
        log.info("  ✓ 课程目标-指标点矩阵: {} 条 (课程{})", count, code);
    }

    private int[][] getIndicatorIndicesForCourse(String code) {
        switch (code) {
            case "CS101": return new int[][]{{0, 1}, {3, 1}, {6, 12}};  // 数据结构
            case "CS102": return new int[][]{{1, 2}, {5, 6}, {16, 33}};  // 操作系统
            case "CS103": return new int[][]{{2, 4}, {16, 7}, {18, 30}}; // 计算机网络
            case "CS104": return new int[][]{{1, 3}, {7, 13}, {33, 8}};  // 数据库
            case "SE101": return new int[][]{{1, 2}, {7, 15}, {27, 30}}; // OOP
            case "SE102": return new int[][]{{3, 12}, {16, 30}, {30, 33}}; // 软件测试
            case "SE103": return new int[][]{{7, 15}, {16, 27}, {27, 30}}; // Web前端
            case "SE104": return new int[][]{{8, 23}, {27, 30}, {33, 8}}; // 项目管理
            default:      return new int[][]{{0, 1}, {3, 6}, {12, 16}};
        }
    }

    // ==================== 19. 课程资源 ====================
    private void initCourseResources(CourseOffering offering) {
        if (resourceRepo.findByOfferingId(offering.getId()).size() > 0) return;

        String[][] resources = {
                {"课程教学大纲.pdf",  "/uploads/syllabus_" + offering.getId() + ".pdf",  "教学大纲"},
                {"课件_第一章.pptx", "/uploads/slides_ch1_" + offering.getId() + ".pptx", "课件"},
                {"实验指导书.docx",  "/uploads/lab_guide_" + offering.getId() + ".docx",  "实验指导"},
                {"习题集.pdf",       "/uploads/exercises_" + offering.getId() + ".pdf",   "习题"},
        };
        for (String[] r : resources) {
            CourseResource cr = new CourseResource();
            cr.setOfferingId(offering.getId());
            cr.setFileName(r[0]);
            cr.setFilePath(r[1]);
            cr.setResourceType(r[2]);
            resourceRepo.save(cr);
        }
    }
}
