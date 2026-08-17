package com.example.bank.customer.controller;

import com.example.bank.common.constants.ErrorCode;
import com.example.bank.common.exception.BusinessException;
import com.example.bank.common.result.Result;
import com.example.bank.customer.entity.SmsMessageEntity;
import com.example.bank.customer.service.SmsMessageService;
import com.example.bank.customer.vo.SmsMessageVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** APIs for observing SMS MQ message lifecycle. */
@RestController
@RequestMapping("/api/sms/messages")
public class SmsMessageController {

    private final SmsMessageService smsMessageService;

    public SmsMessageController(SmsMessageService smsMessageService) {
        this.smsMessageService = smsMessageService;
    }

    @GetMapping
    public Result<List<SmsMessageVO>> list(@RequestParam(value = "customerNo", required = false) String customerNo,
                                           @RequestParam(value = "status", required = false) String status) {
        List<SmsMessageVO> messages = smsMessageService.lambdaQuery()
                .eq(customerNo != null && !customerNo.isBlank(), SmsMessageEntity::getCustomerNo, customerNo)
                .eq(status != null && !status.isBlank(), SmsMessageEntity::getStatus, status)
                .orderByDesc(SmsMessageEntity::getCreateTime)
                .last("LIMIT 50")
                .list()
                .stream()
                .map(this::toVO)
                .toList();
        return Result.success(messages);
    }

    @GetMapping("/{messageId}")
    public Result<SmsMessageVO> detail(@PathVariable("messageId") String messageId) {
        SmsMessageEntity message = smsMessageService.findByMessageId(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "SMS message not found");
        }
        return Result.success(toVO(message));
    }

    private SmsMessageVO toVO(SmsMessageEntity entity) {
        SmsMessageVO vo = new SmsMessageVO();
        vo.setMessageId(entity.getMessageId());
        vo.setCustomerNo(entity.getCustomerNo());
        vo.setAccountNo(entity.getAccountNo());
        vo.setPhone(maskPhone(entity.getPhone()));
        vo.setScene(entity.getScene());
        vo.setContent(entity.getContent());
        vo.setStatus(entity.getStatus());
        vo.setRetryCount(entity.getRetryCount());
        vo.setFailReason(entity.getFailReason());
        vo.setSentAt(entity.getSentAt());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "unknown";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
