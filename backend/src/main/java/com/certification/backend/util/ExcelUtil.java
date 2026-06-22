package com.certification.backend.util;

import com.certification.backend.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
     * <p>
     * 使用 EasyExcel 的同步读取模式。
     *
     * @param file 上传的 Excel 文件
     * @return 解析后的 User 列表（未持久化）
     */
    public static List<User> readUsersFromExcel(MultipartFile file) {
        List<User> userList = new ArrayList<>();

        try (InputStream in = file.getInputStream()) {
            List<List<String>> rows = readAllRows(in);

            for (int i = 1; i < rows.size(); i++) { // 跳过表头（第0行）
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
     * 基于 EasyExcel 同步读取所有行（不依赖监听器）
     */
    private static List<List<String>> readAllRows(InputStream in) {
        // 使用 com.alibaba.excel.EasyExcel 的同步读取
        return com.alibaba.excel.EasyExcel.read(in)
                .headRowNumber(0)
                .sheet()
                .doReadSync();
    }
}