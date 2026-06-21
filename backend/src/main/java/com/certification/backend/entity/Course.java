package com.certification.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "course")
@Data
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String code;

    @Column(length = 100)
    private String name;

    @Column(precision = 4, scale = 1)
    private BigDecimal credits;

    @Column(name = "total_hours")
    private Integer totalHours;

    @Column(name = "theory_hours")
    private Integer theoryHours;

    @Column(name = "lab_hours")
    private Integer labHours;

    @Column(length = 20)
    private String semester;

    @Column(name = "course_type", length = 50)
    private String courseType;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "program_id")
    private Long programId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}