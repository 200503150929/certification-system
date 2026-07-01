package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果返回参数
 */
@Setter
@Getter
public class ImportResultResponse {

    /** 成功导入/移除数量 */
    private int successCount;

    /** 总数量 */
    private int totalCount;

    /** 失败的学生学号/ID列表 */
    private List<String> failedItems = new ArrayList<>();

    /** 错误信息列表 */
    private List<String> errorMessages = new ArrayList<>();

    public ImportResultResponse() {
    }

    public ImportResultResponse(int successCount, int totalCount) {
        this.successCount = successCount;
        this.totalCount = totalCount;
    }

    public ImportResultResponse(int successCount, int totalCount, List<String> failedItems) {
        this.successCount = successCount;
        this.totalCount = totalCount;
        this.failedItems = failedItems != null ? failedItems : new ArrayList<>();
    }

    public void addFailedItem(String item) {
        this.failedItems.add(item);
    }

    public void addErrorMessage(String message) {
        this.errorMessages.add(message);
    }

    public String getSummary() {
        if (successCount == totalCount) {
            return "成功处理 " + successCount + " 条记录";
        } else {
            return "成功 " + successCount + " 条，失败 " + (totalCount - successCount) + " 条";
        }
    }
}