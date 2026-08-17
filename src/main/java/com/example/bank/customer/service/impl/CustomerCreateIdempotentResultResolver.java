package com.example.bank.customer.service.impl;

import com.example.bank.common.constants.ErrorCode;
import com.example.bank.common.exception.BusinessException;
import com.example.bank.common.idempotency.IdempotentResultResolver;
import com.example.bank.common.idempotency.entity.RequestRecordEntity;
import org.springframework.stereotype.Component;

/**
 * Rebuilds the original customer-create response for duplicate APS requests.
 */
@Component
public class CustomerCreateIdempotentResultResolver implements IdempotentResultResolver {

    public static final String BUSINESS_TYPE = "CUSTOMER_CREATE";

    @Override
    public String businessType() {
        return BUSINESS_TYPE;
    }

    @Override
    public Object resolve(RequestRecordEntity record) {
        String customerId = record.getReferenceNo();
        if (customerId == null || customerId.isBlank()) {
            throw new BusinessException(ErrorCode.CONFLICT, "开户请求已处理，但客户编号不存在，请联系管理员确认流水");
        }
        return Long.valueOf(customerId);
    }
}
