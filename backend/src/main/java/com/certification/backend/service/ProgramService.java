package com.certification.backend.service;

import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.request.ProgramRequest;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.ProgramDetailResponse;
import com.certification.backend.dto.response.ProgramResponse;

import java.util.List;

public interface ProgramService {

    // ============ 查询 ============
    PageResult<ProgramResponse> listPrograms(String majorNameFuzzy, String status, PageQuery pageQuery);
    List<ProgramResponse> listAllPrograms();
    ProgramDetailResponse getProgramDetail(Long id);
    ProgramResponse getProgramById(Long id);

    // ============ 增删改 ============
    ProgramResponse addProgram(ProgramRequest request);
    ProgramResponse updateProgram(ProgramRequest request);
    void deleteProgram(Long id);

    // ============ 发布/取消发布 ============
    /**
     * 发布前校验数据完整性
     * @param programId 专业ID
     * @return 错误信息列表，为空表示校验通过
     */
    List<String> validateProgramData(Long programId);

    void publishProgram(Long id);
    void unpublishProgram(Long id);
}