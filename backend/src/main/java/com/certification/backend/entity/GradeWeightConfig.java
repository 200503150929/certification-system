package com.certification.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩权重配置（按开课）
 * 平时成绩、实验报告、期中考试、期末考试 四项权重，合计 = 1.0
 */
@Entity
@Table(name = "grade_weight_config")
@Data
public class GradeWeightConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offering_id", nullable = false, unique = true)
    private Long offeringId;

    /** 平时成绩权重（0~1，默认 0.25） */
    @Column(name = "daily_weight", precision = 5, scale = 4, nullable = false)
    private BigDecimal dailyWeight = new BigDecimal("0.2500");

    /** 实验报告权重（0~1，默认 0.25） */
    @Column(name = "report_weight", precision = 5, scale = 4, nullable = false)
    private BigDecimal reportWeight = new BigDecimal("0.2500");

    /** 期中考试权重（0~1，默认 0.25） */
    @Column(name = "midterm_weight", precision = 5, scale = 4, nullable = false)
    private BigDecimal midtermWeight = new BigDecimal("0.2500");

    /** 期末考试权重（0~1，默认 0.25） */
    @Column(name = "final_weight", precision = 5, scale = 4, nullable = false)
    private BigDecimal finalWeight = new BigDecimal("0.2500");

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
