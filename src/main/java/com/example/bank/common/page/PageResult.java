package com.example.bank.common.page;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 分页返回结果。
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    private long pageNo;
    private long pageSize;
    private long total;
    private long pages;
    private List<T> records;
}
