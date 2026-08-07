package com.example.bank.customer.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户展示对象。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerVO {

    private Long id;
    private String customerNo;
    private String name;
    private String phone;
    private String email;
    private Integer status;
    private String accountOpenStatus;
}
