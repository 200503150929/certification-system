package com.certification.backend.util;

import com.certification.backend.entity.User;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel 工具类（基于 EasyExcel）
 *
 * 支持功能：
 * 1. 用户批量导入：用户名、姓名、角色、电话、邮箱、学院、专业
 * 2. 成绩批量导入：学号、分数
 * 3. 成绩导出到 Excel：学号、姓名、考核环节、分数
 */
public class ExcelUtil {

    private static final Logger log = LoggerFactory.getLogger(ExcelUtil.class);

    private ExcelUtil() {
    }

    /**
     * 简单读取 Excel 并转换为 User 列表
     *
     * @param file 上传的 Excel 文件
     * @return 解析后的 User 列表（未持久化）
     */
    public static List<User> readUsersFromExcel(MultipartFile file) {
        List<User> userList = new ArrayList<>();

        try (InputStream in = file.getInputStream()) {
            List<List<String>> rows = readAllRows(in);

            // 检查第一行是否是表头（包含"用户名"字样）
            int startRow = 0;
            if (!rows.isEmpty() && rows.get(0).size() > 0) {
                String firstCell = rows.get(0).get(0);
                if ("用户名".equals(firstCell) || firstCell.contains("用户")) {
                    startRow = 1; // 跳过表头
                }
            }

            for (int i = startRow; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    continue;
                }

                try {
                    User user = new User();
                    user.setUsername(getCell(row, 0));   // 用户名/工号/学号
                    user.setName(getCell(row, 1));       // 姓名
                    user.setRole(getCell(row, 2));       // 角色
                    user.setPhone(getCell(row, 3));      // 电话
                    user.setEmail(getCell(row, 4));      // 邮箱
                    user.setCollege(getCell(row, 5));    // 学院（原 department）
                    user.setMajor(getCell(row, 6));      // 专业

                    // 基本校验
                    if (user.getUsername() == null || user.getUsername().isEmpty()) {
                        log.warn("Excel 第{}行跳过：用户名为空", i + 1);
                        continue;
                    }
                    if (user.getName() == null || user.getName().isEmpty()) {
                        log.warn("Excel 第{}行跳过：姓名为空", i + 1);
                        continue;
                    }
                    if (user.getRole() == null || user.getRole().isEmpty()) {
                        log.warn("Excel 第{}行跳过：角色为空", i + 1);
                        continue;
                    }

                    userList.add(user);
                } catch (Exception e) {
                    log.warn("Excel 第{}行解析失败: {}", i + 1, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("读取 Excel 文件失败", e);
            throw new RuntimeException("Excel 文件读取失败: " + e.getMessage(), e);
        }

        return userList;
    }

    // ==================== 成绩导入 ====================

    /**
     * 成绩导入行数据结构
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GradeImportRow {
        /** 学号（对应 User 表的 username 字段） */
        private String studentNo;
        /** 分数 */
        private BigDecimal score;
    }

    /**
     * 读取 Excel 成绩文件，返回 GradeImportRow 列表
     *
     * Excel 格式要求：
     * 第 1 行：表头（自动忽略）
     * 第 1 列：学号（studentNo，必填）
     * 第 2 列：分数（score，必填，0-100 的数字）
     *
     * @param file 上传的 Excel 文件
     * @return 解析后的 GradeImportRow 列表（未持久化）
     */
    public static List<GradeImportRow> readGradesFromExcel(MultipartFile file) {
        List<GradeImportRow> result = new ArrayList<>();

        try (InputStream in = file.getInputStream()) {
            List<List<String>> rows = readAllRows(in);

            // 跳过第一行表头（从索引 1 开始）
            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    continue;
                }

                try {
                    // 第 1 列：学号
                    String studentNo = getCell(row, 0);
                    if (studentNo == null || studentNo.isEmpty()) {
                        log.warn("Excel 第{}行跳过：学号为空", i + 1);
                        continue;
                    }

                    // 第 2 列：分数
                    String scoreStr = getCell(row, 1);
                    if (scoreStr == null || scoreStr.isEmpty()) {
                        log.warn("Excel 第{}行跳过：分数为空（学号={}）", i + 1, studentNo);
                        continue;
                    }

                    BigDecimal score;
                    try {
                        score = new BigDecimal(scoreStr);
                    } catch (NumberFormatException e) {
                        log.warn("Excel 第{}行跳过：分数格式错误 '{}'（学号={}）", i + 1, scoreStr, studentNo);
                        continue;
                    }

                    if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
                        log.warn("Excel 第{}行跳过：分数超出0-100范围 {}（学号={}）", i + 1, scoreStr, studentNo);
                        continue;
                    }

                    result.add(new GradeImportRow(studentNo, score));

                } catch (Exception e) {
                    log.warn("Excel 第{}行解析失败: {}", i + 1, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("读取 Excel 文件失败", e);
            throw new RuntimeException("Excel 文件读取失败: " + e.getMessage(), e);
        }

        log.info("Excel 成绩导入解析完成，共解析 {} 条有效记录", result.size());
        return result;
    }

    // ==================== 成绩导出 ====================

    /**
     * 成绩导出行数据，EasyExcel 写入时使用注解定义列顺序和表头
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GradeExportRow {

        @ExcelProperty("学号")
        @ColumnWidth(20)
        private String studentNo;

        @ExcelProperty("姓名")
        @ColumnWidth(15)
        private String studentName;

        @ExcelProperty("考核环节")
        @ColumnWidth(20)
        private String assessmentName;

        @ExcelProperty("分数")
        @ColumnWidth(12)
        private String score;
    }

    /**
     * 将成绩数据写入 Excel 并通过 HttpServletResponse 输出为文件下载
     *
     * @param rows       成绩数据列表
     * @param response   HTTP 响应对象
     * @param fileName   文件名（不含后缀，自动添加 .xlsx）
     */
    public static void writeGradesToExcel(List<GradeExportRow> rows,
                                          HttpServletResponse response,
                                          String fileName) throws IOException {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName + ".xlsx");

        // 使用 EasyExcel 写入数据
        com.alibaba.excel.EasyExcel.write(response.getOutputStream(), GradeExportRow.class)
                .sheet("成绩表")
                .doWrite(rows);
    }

    // ==================== 学生成绩（四列固定成绩）导入/导出 ====================

    /**
     * 学生成绩导入行数据结构（四列固定成绩）
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StudentGradeImportRow {
        /** 学号（对应 User 表的 username 字段） */
        private String studentNo;
        /** 平时成绩 */
        private BigDecimal dailyScore;
        /** 实验报告 */
        private BigDecimal reportScore;
        /** 期中考试 */
        private BigDecimal midtermScore;
        /** 期末考试 */
        private BigDecimal finalScore;
    }

    /**
     * 读取 Excel 学生成绩文件，返回 StudentGradeImportRow 列表
     *
     * Excel 格式要求：
     * 第 1 行：表头（自动跳过）
     * 第 1 列：学号（必填）
     * 第 2 列：平时成绩（选填）
     * 第 3 列：实验报告（选填）
     * 第 4 列：期中考试（选填）
     * 第 5 列：期末考试（选填）
     *
     * @param file 上传的 Excel 文件
     * @return 解析后的 StudentGradeImportRow 列表
     */
    public static List<StudentGradeImportRow> readStudentGradesFromExcel(MultipartFile file) {
        List<StudentGradeImportRow> result = new ArrayList<>();

        try (InputStream in = file.getInputStream()) {
            List<List<String>> rows = readAllRows(in);

            // 跳过第一行表头（从索引 1 开始）
            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    continue;
                }

                try {
                    // 第 1 列：学号
                    String studentNo = getCell(row, 0);
                    if (studentNo == null || studentNo.isEmpty()) {
                        log.warn("Excel 第{}行跳过：学号为空", i + 1);
                        continue;
                    }

                    BigDecimal dailyScore = parseScore(getCell(row, 1));
                    BigDecimal reportScore = parseScore(getCell(row, 2));
                    BigDecimal midtermScore = parseScore(getCell(row, 3));
                    BigDecimal finalScore = parseScore(getCell(row, 4));

                    result.add(new StudentGradeImportRow(studentNo, dailyScore, reportScore, midtermScore, finalScore));

                } catch (Exception e) {
                    log.warn("Excel 第{}行解析失败: {}", i + 1, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("读取 Excel 文件失败", e);
            throw new RuntimeException("Excel 文件读取失败: " + e.getMessage(), e);
        }

        log.info("Excel 学生成绩导入解析完成，共解析 {} 条有效记录", result.size());
        return result;
    }

    /**
     * 解析分数单元格，空值或格式错误返回 null
     */
    private static BigDecimal parseScore(String scoreStr) {
        if (scoreStr == null || scoreStr.trim().isEmpty()) {
            return null;
        }
        try {
            BigDecimal score = new BigDecimal(scoreStr.trim());
            if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
                return null;
            }
            return score;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 学生成绩导出行数据（七列：学号、姓名、四个成绩、最终成绩）
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StudentGradeExportRow {

        @ExcelProperty("学号")
        @ColumnWidth(20)
        private String studentNo;

        @ExcelProperty("姓名")
        @ColumnWidth(15)
        private String studentName;

        @ExcelProperty("平时成绩")
        @ColumnWidth(12)
        private String dailyScore;

        @ExcelProperty("实验报告")
        @ColumnWidth(12)
        private String reportScore;

        @ExcelProperty("期中考试")
        @ColumnWidth(12)
        private String midtermScore;

        @ExcelProperty("期末考试")
        @ColumnWidth(12)
        private String finalScore;

        @ExcelProperty("最终成绩")
        @ColumnWidth(12)
        private String totalScore;
    }

    /**
     * 将学生成绩数据写入 Excel 并通过 HttpServletResponse 输出为文件下载
     *
     * @param rows       成绩数据列表
     * @param response   HTTP 响应对象
     * @param fileName   文件名（不含后缀，自动添加 .xlsx）
     */
    public static void writeStudentGradesToExcel(List<StudentGradeExportRow> rows,
                                                 HttpServletResponse response,
                                                 String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName + ".xlsx");

        com.alibaba.excel.EasyExcel.write(response.getOutputStream(), StudentGradeExportRow.class)
                .sheet("学生成绩表")
                .doWrite(rows);
    }

    // ==================== 私有方法 ====================

    /**
     * 安全获取单元格字符串值
     */
    private static String getCell(List<String> row, int index) {
        if (index >= row.size()) {
            return null;
        }
        String val = row.get(index);
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }

    /**
     * 基于 EasyExcel 同步读取所有行
     * headRowNumber(0) 表示不自动映射表头，返回所有行
     */
    private static List<List<String>> readAllRows(InputStream in) {
        // 读取所有行，包括表头
        List<Map<Integer, String>> rows = com.alibaba.excel.EasyExcel.read(in)
                .headRowNumber(0)  // 0 表示没有表头，所有行都是数据
                .sheet()
                .doReadSync();

        // 将 Map<Integer, String> 转换为 List<String>
        List<List<String>> result = new ArrayList<>();
        for (Map<Integer, String> rowMap : rows) {
            List<String> rowList = new ArrayList<>();
            // 按列索引顺序取出值
            int maxIndex = 0;
            for (Integer key : rowMap.keySet()) {
                if (key > maxIndex) {
                    maxIndex = key;
                }
            }
            for (int i = 0; i <= maxIndex; i++) {
                rowList.add(rowMap.get(i));
            }
            result.add(rowList);
        }
        return result;
    }


    /**
     * 从 Excel 文件中读取学号列表（只读第一列）
     *
     * Excel 格式要求：
     * 第 1 行：表头（自动跳过）
     * 第 1 列：学号
     *
     * @param file 上传的 Excel 文件
     * @return 学号列表
     */
    public static List<String> readStudentNosFromExcel(MultipartFile file) {
        List<String> studentNos = new ArrayList<>();

        try (InputStream in = file.getInputStream()) {
            List<List<String>> rows = readAllRows(in);

            // 跳过第一行表头（从索引 1 开始）
            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    continue;
                }

                // 第 1 列：学号
                String studentNo = getCell(row, 0);
                if (studentNo == null || studentNo.isEmpty()) {
                    continue;
                }

                studentNos.add(studentNo.trim());
            }
        } catch (IOException e) {
            log.error("读取 Excel 文件失败", e);
            throw new RuntimeException("Excel 文件读取失败: " + e.getMessage(), e);
        }

        log.info("Excel 学号导入解析完成，共解析 {} 条记录", studentNos.size());
        return studentNos;
    }
}