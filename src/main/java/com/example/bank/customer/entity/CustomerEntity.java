package com.example.bank.customer.entity;

import com.example.bank.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_customer")
public class CustomerEntity extends BaseEntity {

    private String customerNo;
    private String name;
    private String phone;
    private String email;
    private Integer status;
    private String accountOpenStatus;
}
