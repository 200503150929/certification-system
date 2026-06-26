package com.certification.backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 课程目标达成度结果 DTO
 */
@Data
public class ObjectiveAchievementDTO {

    /** 课程目标ID */
    private Long objectiveId;

    /** 课程目标描述 */
    private String description;

    /** 课程目标权重 */
    private BigDecimal weight;

    /** 班级平均达成度（0~1之间的值） */
    private BigDecimal classAchievement;

    /** 关联的考核环节数量 */
    private int assessmentCount;

    /** 参与计算的学生数量 */
    private int studentCount;

    /** 各学生个体达成度列表 */
    private List<StudentAchievement> studentAchievements;

    /**
     * 学生个体达成度
     */
    @Data
    public static class StudentAchievement {
        /** 学生ID */
        private Long studentId;

        /** 学生姓名/学号 */
        private String studentName;

        /** 学生学号 */
        private String studentNo;

        /** 该学生在此课程目标上的达成度（0~1之间） */
        private BigDecimal achievement;
    }
}
