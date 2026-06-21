package com.certification.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "course_requirement_matrix")
@Data
public class CourseRequirementMatrix {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "indicator_id")
    private Long indicatorId;

    @Column(name = "support_level", length = 10)
    private String supportLevel; // H/M/L
}