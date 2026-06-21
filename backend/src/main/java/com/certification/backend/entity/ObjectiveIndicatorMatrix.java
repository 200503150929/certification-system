package com.certification.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "objective_indicator_matrix")
@Data
public class ObjectiveIndicatorMatrix {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "objective_id")
    private Long objectiveId;

    @Column(name = "indicator_id")
    private Long indicatorId;

    @Column(name = "support_level", length = 10)
    private String supportLevel;
}