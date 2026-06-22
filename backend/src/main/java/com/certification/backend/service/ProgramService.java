package com.certification.backend.service;

import com.certification.backend.dto.request.ProgramRequest;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.ProgramDetailResponse;
import com.certification.backend.dto.response.ProgramResponse;
import com.certification.backend.dto.request.PageQuery;

import java.util.List;

/**
 * 专业/培养方案业务接口
 */
public interface ProgramService {

    /**
     * 分页查询专业列表
     */
    PageResult<ProgramResponse> listPrograms(String majorNameFuzzy, String status, PageQuery pageQuery);

    /**
     * 查询所有专业列表（不分页）
     */
    List<ProgramResponse> listAllPrograms();

    /**
     * 查询专业详情（含完整的培养目标、毕业要求、指标点层级）
     */
    ProgramDetailResponse getProgramDetail(Long id);

    /**
     * 查询专业基本信息
     */
    ProgramResponse getProgramById(Long id);

    /**
     * 新增专业
     */
    ProgramResponse addProgram(ProgramRequest request);

    /**
     * 编辑专业
     */
    ProgramResponse updateProgram(ProgramRequest request);

    /**
     * 删除专业
     */
    void deleteProgram(Long id);

    /**
     * 发布专业（将状态从 draft 改为 published）
     */
    void publishProgram(Long id);

    /**
     * 取消发布（将状态从 published 改为 draft）
     */
    void unpublishProgram(Long id);
}