package com.example.bank.common.result;

import com.example.bank.common.constants.ErrorCode;
import lombok.Getter;

/**
 * 统一返回体。
 */
@Getter
public class Result<T> {

    private final String code;
    private final String message;
    private final T data;

    private Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS, "success", null);
    }

    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(code, message, null);
    }
}
