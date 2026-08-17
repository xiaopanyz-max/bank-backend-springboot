package com.example.bank.common.idempotency;

import com.example.bank.common.idempotency.entity.RequestRecordEntity;

public interface IdempotentResultResolver {

    String businessType();

    Object resolve(RequestRecordEntity record);
}
