package com.certification.backend.dto.request;

/**
 * 分页查询基类
 */
public class PageQuery {

    private int pageNum = 1;
    private int pageSize = 15;

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}