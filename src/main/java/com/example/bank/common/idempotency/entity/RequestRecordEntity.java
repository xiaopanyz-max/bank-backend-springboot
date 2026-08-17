package com.example.bank.common.idempotency.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.bank.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_request_record")
public class RequestRecordEntity extends BaseEntity {

    private String globalSerialNo;
    private String businessType;
    private String requestHash;
    private String status;
    private String referenceNo;
    private String failReason;
}
