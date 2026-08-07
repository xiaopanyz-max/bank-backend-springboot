package com.example.bank.transaction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.bank.common.base.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_transaction")
public class TransactionEntity extends BaseEntity {

    private String transactionNo;
    private Long accountId;
    private BigDecimal amount;
    private String transactionType;
    private String status;
}
