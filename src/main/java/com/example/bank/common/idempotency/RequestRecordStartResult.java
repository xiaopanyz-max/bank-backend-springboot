package com.example.bank.common.idempotency;

import com.example.bank.common.idempotency.entity.RequestRecordEntity;

public record RequestRecordStartResult(boolean firstRequest, RequestRecordEntity record) {
}
