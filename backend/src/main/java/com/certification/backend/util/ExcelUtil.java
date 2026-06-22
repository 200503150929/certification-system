package com.certification.backend.util;

import com.certification.backend.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel 导入工具类（基于 EasyExcel），读取 Excel 批量转换为 User 实体
 *
 * 预期 Excel 列顺序：
 * 用户名（必填）、姓名（必填）、角色（必填，admin/teacher/student）、
 * 电话、邮箱、院系/部门
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

            // rows 现在包含所有数据行，没有表头（因为 headRowNumber(0)）
            // 如果使用 headRowNumber(0)，则 rows 包含所有行，包括表头
            // 所以第一行就是数据，但你的 Excel 有表头，所以应该从第1行开始
            int startRow = 0;
            // 检查第一行是否是表头（包含"用户名"字样）
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
                    user.setUsername(getCell(row, 0));
                    user.setName(getCell(row, 1));
                    user.setRole(getCell(row, 2));
                    user.setPhone(getCell(row, 3));
                    user.setEmail(getCell(row, 4));
                    user.setDepartment(getCell(row, 5));

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
}