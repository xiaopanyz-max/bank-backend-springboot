package com.example.bank.common.constants;

/**
 * 全局错误码常量。
 */
public final class ErrorCode {

    private ErrorCode() {
    }

    public static final String SUCCESS = "00000";
    public static final String BAD_REQUEST = "40000";
    public static final String UNAUTHORIZED = "40100";
    public static final String FORBIDDEN = "40300";
    public static final String NOT_FOUND = "40400";
    public static final String CONFLICT = "40900";
    public static final String SYSTEM_ERROR = "50000";
    public static final String SERVICE_UNAVAILABLE = "50300";
    public static final String VALIDATION_ERROR = "40001";
    public static final String BUSINESS_ERROR = "40002";
}
