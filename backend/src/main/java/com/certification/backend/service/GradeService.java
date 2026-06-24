package com.certification.backend.service;

import com.certification.backend.dto.request.GradeRequest;
import com.certification.backend.dto.response.GradeResponse;
import com.certification.backend.util.ExcelUtil;

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

    /**
     * Excel 批量导入成绩
     *
     * @param assessmentId   考核环节 ID
     * @param gradeRows      Excel 解析后的成绩行列表
     * @param teacherUsername 当前教师用户名
     * @return 成功导入的条数
     */
    int importGrades(Long assessmentId, List<ExcelUtil.GradeImportRow> gradeRows, String teacherUsername);
}
