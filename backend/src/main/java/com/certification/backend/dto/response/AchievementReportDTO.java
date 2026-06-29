package com.certification.backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 达成度评价报告 DTO
 */
@Data
public class AchievementReportDTO {

    /** 课程基本信息 */
    private CourseInfo courseInfo;

    /** 教师信息 */
    private TeacherInfo teacherInfo;

    /** 学生人数 */
    private int studentCount;

    /** 各课程目标达成度列表 */
    private List<ObjectiveAchievementDTO> objectiveAchievements;

    /** 各毕业要求达成度列表 */
    private List<GraduationAchievementDTO> graduationAchievements;

    /** 考核结构信息 */
    private List<AssessmentStructure> assessmentStructures;

    @Data
    public static class CourseInfo {
        private Long courseId;
        private String courseCode;
        private String courseName;
        private BigDecimal credits;
        private String academicYear;
        private String semester;
    }

    @Data
    public static class TeacherInfo {
        private Long teacherId;
        private String name;
        private String college;
    }

    @Data
    public static class AssessmentStructure {
        private Long assessmentId;
        private String name;
        private BigDecimal weight;
        private String linkedObjective;
        private BigDecimal averageScore;
        private BigDecimal scoreRate;
    }
}
