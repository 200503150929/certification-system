package com.certification.backend.service;

import com.certification.backend.dto.request.GradeRequest;
import com.certification.backend.dto.response.GradeResponse;

import java.util.List;

/**
 * 成绩管理业务接口
 */
public interface GradeService {

    /**
     * 录入学生成绩
     */
    GradeResponse add(GradeRequest request, String username);

    /**
     * 查询指定开课记录下的所有成绩
     */
    List<GradeResponse> listByOfferingId(Long offeringId, String username);

    /**
     * 修改成绩
     */
    GradeResponse update(Long id, GradeRequest request, String username);
}
