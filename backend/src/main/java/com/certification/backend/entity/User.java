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

    @Column(unique = true, nullable = false, length = 12) //工号/学号/管理员账号
    private String username;

    @Column(nullable = false, length = 20)
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
    private String department;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // JPA自动填充创建时间
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}