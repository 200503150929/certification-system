package com.certification.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 分页查询基类
 */
@Setter
@Getter
public class PageQuery {

    private int pageNum = 1;
    private int pageSize = 15;

}