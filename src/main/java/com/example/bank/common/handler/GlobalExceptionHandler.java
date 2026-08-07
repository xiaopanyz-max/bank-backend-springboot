package com.example.bank.common.handler;

import com.example.bank.common.constants.ErrorCode;
import com.example.bank.common.exception.BusinessException;
import com.example.bank.common.exception.ServiceUnavailableException;
import com.example.bank.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.badRequest().body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Result<Void>> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.warn("Downstream service unavailable: {}", ex.getCause() == null ? ex.getMessage() : ex.getCause().toString());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.fail(ErrorCode.SERVICE_UNAVAILABLE, ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<Result<Void>> handleValidationException(Exception ex) {
        String message = extractValidationMessage(ex);
        log.warn("Request validation failed: {}", message);
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadableRequestBody(HttpMessageNotReadableException ex) {
        log.warn("Request body parsing failed: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(Result.fail(
                ErrorCode.BAD_REQUEST,
                "请求体格式不正确：请使用 application/json，并确认 JSON 语法、字段名和字段类型正确。"));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKeyException(DuplicateKeyException ex) {
        log.warn("Duplicate database key: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.fail(ErrorCode.CONFLICT, "数据已存在，请勿重复提交"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result<Void>> handleDataAccessException(DataAccessException ex) {
        log.error("Database error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.SYSTEM_ERROR, "数据库访问异常"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试"));
    }

    private String extractValidationMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException manve) {
            return manve.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + fieldError.getDefaultMessage())
                    .collect(Collectors.joining("；"));
        }
        if (ex instanceof BindException bindException) {
            return bindException.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + fieldError.getDefaultMessage())
                    .collect(Collectors.joining("；"));
        }
        if (ex instanceof ConstraintViolationException cve) {
            return cve.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + violation.getMessage())
                    .collect(Collectors.joining("；"));
        }
        return "参数校验失败";
    }
}
