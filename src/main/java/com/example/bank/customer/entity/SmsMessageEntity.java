package com.example.bank.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.bank.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Records the full lifecycle of an asynchronous SMS message. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sms_message")
public class SmsMessageEntity extends BaseEntity {

    private String messageId;
    private String customerNo;
    private String accountNo;
    private String phone;
    private String scene;
    private String content;
    private String status;
    private Integer retryCount;
    private String failReason;
    private LocalDateTime sentAt;
}
