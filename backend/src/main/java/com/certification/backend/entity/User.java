package com.certification.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 12)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 10)
    private String name;

    @Column(length = 20)
    private String role; // admin, teacher, student

    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    private String email;

    @Column(length = 50)
    private String college;  // 学院

    @Column(length = 50)
    private String major;    // 专业

    // 学生特有字段
    @Column(length = 20)
    private String grade;    // 年级，如 2024级

    @Column(name = "class_name", length = 20)
    private String className;// 班级，如 1班


    @Column(nullable = false)
    private Integer status = 1;

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