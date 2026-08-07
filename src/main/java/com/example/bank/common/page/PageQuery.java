package com.example.bank.common.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询基础参数。
 */
@Data
public class PageQuery {

    @Min(1)
    private long pageNo = 1L;

    @Min(1)
    @Max(500)
    private long pageSize = 10L;
}
