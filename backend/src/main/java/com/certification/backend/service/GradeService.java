package com.certification.backend.service;

import com.certification.backend.dto.request.GradeRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.GradeResponse;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.util.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
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

    /**
     * 导出成绩列表到 Excel 文件
     *
     * @param offeringId 开课 ID
     * @param username   当前教师用户名
     * @param response   HTTP 响应对象（用于写入文件流）
     * @throws IOException 写入响应流时异常
     */
    void exportGrades(Long offeringId, String username, HttpServletResponse response) throws IOException;

    /**
     * 保存/更新成绩（智能判断新增或更新）
     * <p>
     * 逻辑：
     * - 若 request.id 有值，则按 ID 更新
     * - 若 request.id 为空，按 (assessmentId + studentId) 查找：存在则更新，不存在则插入
     *
     * @param request  成绩请求参数
     * @param username 当前教师用户名
     * @return 保存后的成绩响应
     */
    GradeResponse saveOrUpdate(GradeRequest request, String username);

    /**
     * 分页查询成绩列表（支持按学生姓名模糊搜索）
     *
     * @param offeringId  开课 ID
     * @param studentName 学生姓名（模糊搜索，可选）
     * @param pageQuery   分页参数
     * @param username    当前教师用户名
     * @return 分页结果
     */
    PageResult<GradeResponse> listGrades(Long offeringId, String studentName,
                                         PageQuery pageQuery, String username);

    /**
     * 删除成绩记录
     *
     * @param id       成绩记录 ID
     * @param username 当前教师用户名
     */
    void deleteGrade(Long id, String username);
}
