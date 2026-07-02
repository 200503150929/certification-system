package com.certification.backend.service;

import com.certification.backend.dto.request.GradeWeightRequest;
import com.certification.backend.dto.request.StudentGradeBatchRequest;
import com.certification.backend.dto.response.GradeWeightResponse;
import com.certification.backend.dto.response.StudentGradeResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学生成绩服务接口（四列固定成绩）
 */
public interface StudentGradeService {

    /**
     * 查询某门开课下所有学生的成绩（含未录入成绩的学生）
     * 学生列表来源于 student_course 选课记录
     */
    List<StudentGradeResponse> listByOffering(Long offeringId, String username);

    /**
     * 批量保存/更新学生成绩
     * 计算最终成绩（加权平均），并同步到 Grade 表
     */
    List<StudentGradeResponse> saveBatch(StudentGradeBatchRequest request, String username);

    /**
     * Excel 批量导入学生成绩
     */
    int importGrades(Long offeringId, MultipartFile file, String username);

    /**
     * 导出学生成绩到 Excel
     */
    void exportGrades(Long offeringId, String username, HttpServletResponse response) throws java.io.IOException;

    /**
     * 查询单个学生在指定开课下的成绩（学生端使用）
     * 验证学生是否选修了该课程
     */
    StudentGradeResponse getStudentGrade(Long offeringId, String username);

    /**
     * 获取开课的权重配置（无配置时返回默认 25%/25%/25%/25%）
     */
    GradeWeightResponse getWeightConfig(Long offeringId, String username);

    /**
     * 保存开课的权重配置
     */
    GradeWeightResponse saveWeightConfig(GradeWeightRequest request, String username);
}
