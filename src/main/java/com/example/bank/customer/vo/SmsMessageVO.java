package com.example.bank.customer.vo;

import java.time.LocalDateTime;
import lombok.Data;

/** SMS message lifecycle view. */
@Data
public class SmsMessageVO {

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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
