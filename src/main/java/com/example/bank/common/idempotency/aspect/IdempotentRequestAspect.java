package com.example.bank.common.idempotency.aspect;

import com.example.bank.common.constants.ErrorCode;
import com.example.bank.common.dto.BaseRequestDTO;
import com.example.bank.common.exception.BusinessException;
import com.example.bank.common.idempotency.IdempotentResultResolver;
import com.example.bank.common.idempotency.RequestRecordStartResult;
import com.example.bank.common.idempotency.RequestRecordStatus;
import com.example.bank.common.idempotency.annotation.ApsIdempotent;
import com.example.bank.common.idempotency.entity.RequestRecordEntity;
import com.example.bank.common.idempotency.service.RequestRecordService;
import com.example.bank.common.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class IdempotentRequestAspect {

    private final RequestRecordService requestRecordService;
    private final ObjectMapper objectMapper;
    private final Map<String, IdempotentResultResolver> resultResolvers;

    public IdempotentRequestAspect(RequestRecordService requestRecordService,
                                   ObjectMapper objectMapper,
                                   List<IdempotentResultResolver> resultResolvers) {
        this.requestRecordService = requestRecordService;
        this.objectMapper = objectMapper;
        this.resultResolvers = resultResolvers.stream()
                .collect(Collectors.toMap(IdempotentResultResolver::businessType, Function.identity()));
    }

    @Around("execution(* com.example.bank..controller..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Optional<ApsIdempotent> optionalAnnotation = findApsIdempotent(joinPoint);
        if (optionalAnnotation.isEmpty()) {
            return joinPoint.proceed();
        }

        Optional<BaseRequestDTO> optionalRequest = findIdempotentRequest(joinPoint);
        if (optionalRequest.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "幂等接口请求体必须继承 BaseRequestDTO");
        }

        BaseRequestDTO request = optionalRequest.get();
        ApsIdempotent apsIdempotent = optionalAnnotation.get();
        String globalSerialNo = request.getGlobalSerialNo();
        if (globalSerialNo == null || globalSerialNo.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "全局流水号不能为空");
        }
        String requestHash = sha256(toJson(request));

        RequestRecordStartResult startResult =
                requestRecordService.start(apsIdempotent.businessType(), globalSerialNo, requestHash);
        if (!startResult.firstRequest()) {
            Object duplicatedResult = resolveDuplicatedResult(apsIdempotent.businessType(), startResult.record());
            return adaptControllerReturn(joinPoint, duplicatedResult);
        }

        try {
            Object result = joinPoint.proceed();
            requestRecordService.markSuccess(globalSerialNo, extractReferenceNo(result, globalSerialNo));
            return result;
        } catch (Throwable ex) {
            requestRecordService.markFailed(globalSerialNo, ex.getMessage());
            throw ex;
        }
    }

    private Optional<BaseRequestDTO> findIdempotentRequest(ProceedingJoinPoint joinPoint) {
        return Arrays.stream(joinPoint.getArgs())
                .filter(BaseRequestDTO.class::isInstance)
                .map(BaseRequestDTO.class::cast)
                .findFirst();
    }

    private Optional<ApsIdempotent> findApsIdempotent(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method interfaceMethod = signature.getMethod();
        if (interfaceMethod.isAnnotationPresent(ApsIdempotent.class)) {
            return Optional.of(interfaceMethod.getAnnotation(ApsIdempotent.class));
        }

        try {
            Method targetMethod = joinPoint.getTarget().getClass()
                    .getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            if (targetMethod.isAnnotationPresent(ApsIdempotent.class)) {
                return Optional.of(targetMethod.getAnnotation(ApsIdempotent.class));
            }
        } catch (NoSuchMethodException ignored) {
            // Fall back to class-level annotations below.
        }

        Class<?> targetClass = joinPoint.getTarget().getClass();
        if (targetClass.isAnnotationPresent(ApsIdempotent.class)) {
            return Optional.of(targetClass.getAnnotation(ApsIdempotent.class));
        }
        return Optional.empty();
    }

    private Object resolveDuplicatedResult(String businessType, RequestRecordEntity record) {
        if (RequestRecordStatus.PROCESSING.equals(record.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "请求正在处理中，请勿重复提交");
        }
        IdempotentResultResolver resolver = resultResolvers.get(businessType);
        if (resolver == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "请求已处理，请勿重复提交");
        }
        return resolver.resolve(record);
    }

    private Object adaptControllerReturn(ProceedingJoinPoint joinPoint, Object duplicatedResult) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        if (Result.class.isAssignableFrom(signature.getReturnType()) && !(duplicatedResult instanceof Result<?>)) {
            return Result.success(duplicatedResult);
        }
        return duplicatedResult;
    }

    private String extractReferenceNo(Object result, String fallback) {
        Object data = result instanceof Result<?> apiResult ? apiResult.getData() : result;
        if (data == null) {
            return fallback;
        }
        if (data instanceof CharSequence || data instanceof Number) {
            return data.toString();
        }
        try {
            Method method = data.getClass().getMethod("transactionNo");
            Object value = method.invoke(data);
            return value == null ? fallback : value.toString();
        } catch (ReflectiveOperationException ex) {
            return fallback;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求幂等内容序列化失败");
        }
    }

    private String sha256(String raw) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
