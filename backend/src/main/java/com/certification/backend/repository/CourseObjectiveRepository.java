package com.certification.backend.repository;

import com.certification.backend.entity.CourseObjective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 课程目标数据访问接口
 */
@Repository
public interface CourseObjectiveRepository extends JpaRepository<CourseObjective, Long> {

    /**
     * 根据开课ID查询课程目标列表
     */
    List<CourseObjective> findByOfferingId(Long offeringId);

    /**
     * 根据开课ID删除所有课程目标
     */
    void deleteByOfferingId(Long offeringId);

    /**
     * 根据开课ID列表批量查询课程目标
     */
    List<CourseObjective> findByOfferingIdIn(List<Long> offeringIds);
}
