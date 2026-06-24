package com.certification.backend.repository;

import com.certification.backend.entity.CourseResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 课程资源数据访问接口
 */
@Repository
public interface CourseResourceRepository extends JpaRepository<CourseResource, Long> {

    /**
     * 根据开课ID查询资源列表
     */
    List<CourseResource> findByOfferingId(Long offeringId);

    /**
     * 根据开课ID删除所有资源
     */
    void deleteByOfferingId(Long offeringId);
}
