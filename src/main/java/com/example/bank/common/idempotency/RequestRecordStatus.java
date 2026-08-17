package com.example.bank.common.idempotency;

public final class RequestRecordStatus {

    private RequestRecordStatus() {
    }

    public static final String PROCESSING = "PROCESSING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
}
