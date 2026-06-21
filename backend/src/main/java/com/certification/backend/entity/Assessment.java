package com.certification.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessment")
@Data
public class Assessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offering_id")
    private Long offeringId;

    @Column(length = 50)
    private String name;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "objective_id")
    private Long objectiveId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}