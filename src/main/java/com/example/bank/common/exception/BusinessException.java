package com.example.bank.common.exception;

import com.example.bank.common.constants.ErrorCode;
import lombok.Getter;

/**
 * 业务异常，承载错误码和用户可理解消息。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String message) {
        this(ErrorCode.BUSINESS_ERROR, message);
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
