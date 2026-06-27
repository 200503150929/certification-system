package com.certification.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生成绩实体（按学生+开课去规范化存储）
 * 一行记录包含学生在某门开课下的四个固定成绩项
 */
@Entity
@Table(name = "student_grade",
       uniqueConstraints = @UniqueConstraint(columnNames = {"offering_id", "student_id"}))
@Data
public class StudentGrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offering_id", nullable = false)
    private Long offeringId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** 平时成绩 */
    @Column(name = "daily_score", precision = 5, scale = 2)
    private BigDecimal dailyScore;

    /** 实验报告 */
    @Column(name = "report_score", precision = 5, scale = 2)
    private BigDecimal reportScore;

    /** 期中考试 */
    @Column(name = "midterm_score", precision = 5, scale = 2)
    private BigDecimal midtermScore;

    /** 期末考试 */
    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    /** 最终成绩（所有非空组件成绩的平均值） */
    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
