package com.certification.backend.repository;

import com.certification.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 根据用户名查询用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 判断用户名是否已存在
     */
    boolean existsByUsername(String username);

    /**
     * 根据姓名模糊查询用户列表
     */
    java.util.List<User> findByNameContaining(String name);
    // UserRepository.java 新增

    /**
     * 查询所有教师
     */
    List<User> findByRole(String role);

    // UserRepository.java
    List<User> findByUsernameInAndRole(List<String> usernames, String role);
}