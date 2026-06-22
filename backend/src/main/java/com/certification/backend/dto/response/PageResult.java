package com.certification.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 统一分页结果
 */
@Setter
@Getter
public class PageResult<T> {

    private long total;
    private int pageNum;
    private int pageSize;
    private List<T> list;

    public PageResult() {
    }

    public PageResult(long total, int pageNum, int pageSize, List<T> list) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list;
    }

}