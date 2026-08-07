package com.example.bank.customer.dto;

import com.example.bank.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户分页查询入参。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerPageQueryDTO extends PageQuery {

    private String keyword;
}
