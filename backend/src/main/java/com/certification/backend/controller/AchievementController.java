package com.certification.backend.controller;

import com.certification.backend.dto.response.*;
import com.certification.backend.service.AchievementCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 达成度计算与分析控制器
 */
@Tag(name = "09-达成度计算与分析", description = "课程目标达成度、指标点达成度、毕业要求达成度、评价报告")
@RestController
@RequestMapping("/achievement")
public class AchievementController {

    private final AchievementCalculationService achievementService;

    public AchievementController(AchievementCalculationService achievementService) {
        this.achievementService = achievementService;
    }

    @Operation(summary = "计算课程目标达成度", description = "根据开课记录ID，计算各课程目标的班级达成度和学生个体达成度")
    @GetMapping("/objective/{offeringId}")
    public ResponseVO<List<ObjectiveAchievementDTO>> calculateObjective(@PathVariable Long offeringId) {
        List<ObjectiveAchievementDTO> result = achievementService.calculateObjectiveAchievement(offeringId);
        return ResponseVO.success(result);
    }

    @Operation(summary = "计算指标点达成度", description = "根据指标点ID，计算指标点达成度（按支撑课程目标加权平均）")
    @GetMapping("/indicator/{indicatorId}")
    public ResponseVO<IndicatorAchievementDTO> calculateIndicator(@PathVariable Long indicatorId) {
        IndicatorAchievementDTO result = achievementService.calculateIndicatorAchievement(indicatorId);
        return ResponseVO.success(result);
    }

    @Operation(summary = "计算毕业要求达成度", description = "根据毕业要求ID，计算毕业要求整体达成度（取各指标点最小值）")
    @GetMapping("/graduation/{requirementId}")
    public ResponseVO<GraduationAchievementDTO> calculateGraduation(@PathVariable Long requirementId) {
        GraduationAchievementDTO result = achievementService.calculateGraduationAchievement(requirementId);
        return ResponseVO.success(result);
    }

    @Operation(summary = "生成评价报告", description = "整合课程信息、课程目标达成度、毕业要求达成度、考核结构等")
    @GetMapping("/report/{offeringId}")
    public ResponseVO<AchievementReportDTO> generateReport(@PathVariable Long offeringId) {
        AchievementReportDTO report = achievementService.generateReport(offeringId);
        return ResponseVO.success(report);
    }

    @Operation(summary = "导出评价报告（Excel）", description = "将评价报告导出为Excel文件供下载")
    @GetMapping("/export/{offeringId}")
    public void exportReport(@PathVariable Long offeringId, HttpServletResponse response) throws IOException {
        AchievementReportDTO report = achievementService.generateReport(offeringId);
        if (report == null) {
            response.setStatus(404);
            response.getWriter().write("{\"code\":404,\"info\":\"开课记录不存在\"}");
            return;
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("达成度评价报告", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename*=utf-8''" + fileName + ".xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            createReportSheet(workbook, report);
            workbook.write(response.getOutputStream());
        }
    }

    private void createReportSheet(Workbook workbook, AchievementReportDTO report) {
        Sheet sheet = workbook.createSheet("达成度评价报告");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        int rowIdx = 0;

        // 标题行
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("工程教育认证 - 课程达成度评价报告");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));
        rowIdx++;

        // 课程基本信息
        AchievementReportDTO.CourseInfo ci = report.getCourseInfo();
        if (ci != null) {
            rowIdx = addInfoRow(sheet, rowIdx, "课程代码", ci.getCourseCode(), headerStyle, dataStyle);
            rowIdx = addInfoRow(sheet, rowIdx, "课程名称", ci.getCourseName(), headerStyle, dataStyle);
            rowIdx = addInfoRow(sheet, rowIdx, "学年学期",
                    (ci.getAcademicYear() != null ? ci.getAcademicYear() : "") + " " +
                            (ci.getSemester() != null ? ci.getSemester() : ""), headerStyle, dataStyle);
        }
        AchievementReportDTO.TeacherInfo ti = report.getTeacherInfo();
        if (ti != null) {
            rowIdx = addInfoRow(sheet, rowIdx, "授课教师", ti.getName(), headerStyle, dataStyle);
        }
        rowIdx = addInfoRow(sheet, rowIdx, "学生人数", String.valueOf(report.getStudentCount()), headerStyle, dataStyle);
        rowIdx = addInfoRow(sheet, rowIdx, "生成时间", LocalDateTime.now().format(dtf), headerStyle, dataStyle);
        rowIdx++;

        // 课程目标达成度明细
        Row sectionRow = sheet.createRow(rowIdx++);
        sectionRow.createCell(0).setCellValue("一、课程目标达成度明细");
        sectionRow.getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 5));

        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"课程目标", "权重", "班级达成度", "关联考核环节数", "学生人数"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        if (report.getObjectiveAchievements() != null) {
            for (ObjectiveAchievementDTO obj : report.getObjectiveAchievements()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(obj.getDescription());
                row.createCell(1).setCellValue(obj.getWeight() != null ? obj.getWeight().doubleValue() : 0);
                row.createCell(2).setCellValue(obj.getClassAchievement() != null ? obj.getClassAchievement().doubleValue() : 0);
                row.createCell(3).setCellValue(obj.getAssessmentCount());
                row.createCell(4).setCellValue(obj.getStudentCount());
            }
        }
        rowIdx++;

        // 毕业要求达成度汇总
        Row gradSection = sheet.createRow(rowIdx++);
        gradSection.createCell(0).setCellValue("二、毕业要求达成度汇总");
        gradSection.getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 5));

        Row gradHeader = sheet.createRow(rowIdx++);
        String[] gradHeaders = {"毕业要求编号", "描述", "整体达成度", "是否达标"};
        for (int i = 0; i < gradHeaders.length; i++) {
            Cell cell = gradHeader.createCell(i);
            cell.setCellValue(gradHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        if (report.getGraduationAchievements() != null) {
            for (GraduationAchievementDTO grad : report.getGraduationAchievements()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(grad.getCode());
                row.createCell(1).setCellValue(grad.getDescription());
                row.createCell(2).setCellValue(grad.getOverallAchievement() != null
                        ? grad.getOverallAchievement().doubleValue() : 0);
                row.createCell(3).setCellValue(grad.isPassed() ? "是" : "否");
            }
        }
        rowIdx++;

        // 考核结构信息
        Row assessSection = sheet.createRow(rowIdx++);
        assessSection.createCell(0).setCellValue("三、考核结构信息");
        assessSection.getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 5));

        Row assessHeader = sheet.createRow(rowIdx++);
        String[] assessHeaders = {"考核环节", "权重", "关联课程目标", "平均分", "得分率"};
        for (int i = 0; i < assessHeaders.length; i++) {
            Cell cell = assessHeader.createCell(i);
            cell.setCellValue(assessHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        if (report.getAssessmentStructures() != null) {
            for (AchievementReportDTO.AssessmentStructure as : report.getAssessmentStructures()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(as.getName());
                row.createCell(1).setCellValue(as.getWeight() != null ? as.getWeight().doubleValue() : 0);
                row.createCell(2).setCellValue(as.getLinkedObjective());
                row.createCell(3).setCellValue(as.getAverageScore() != null ? as.getAverageScore().doubleValue() : 0);
                row.createCell(4).setCellValue(as.getScoreRate() != null ? as.getScoreRate().doubleValue() : 0);
            }
        }

        // 自动调整列宽
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private int addInfoRow(Sheet sheet, int rowIdx, String label, String value,
                            CellStyle headerStyle, CellStyle dataStyle) {
        Row row = sheet.createRow(rowIdx++);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(headerStyle);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(dataStyle);
        return rowIdx;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}