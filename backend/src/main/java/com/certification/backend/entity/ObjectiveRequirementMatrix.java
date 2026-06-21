package com.certification.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "objective_requirement_matrix")
@Data
public class ObjectiveRequirementMatrix {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "objective_id")
    private Long objectiveId;

    @Column(name = "requirement_id")
    private Long requirementId;

    @Column(name = "support_level", length = 10)
    private String supportLevel; // 强/弱
}