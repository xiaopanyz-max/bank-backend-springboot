package com.example.bank.common.idempotency.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bank.common.constants.ErrorCode;
import com.example.bank.common.dto.BaseRequestDTO;
import com.example.bank.common.exception.BusinessException;
import com.example.bank.common.idempotency.RequestRecordStartResult;
import com.example.bank.common.idempotency.RequestRecordStatus;
import com.example.bank.common.idempotency.annotation.ApsIdempotent;
import com.example.bank.common.idempotency.entity.RequestRecordEntity;
import com.example.bank.common.idempotency.service.RequestRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

class IdempotentRequestAspectTest {

    private final RequestRecordService requestRecordService = mock(RequestRecordService.class);
    private final IdempotentRequestAspect aspect =
            new IdempotentRequestAspect(requestRecordService, new ObjectMapper());

    @Test
    void duplicateFinishedRequestShouldRejectInsteadOfReplayResult() throws Throwable {
        TestRequest request = new TestRequest();
        request.setGlobalSerialNo("serial-001");
        RequestRecordEntity record = new RequestRecordEntity();
        record.setGlobalSerialNo("serial-001");
        record.setBusinessType("CUSTOMER_CREATE");
        record.setStatus(RequestRecordStatus.SUCCESS);
        record.setReferenceNo("customer-001");

        ProceedingJoinPoint joinPoint = joinPointFor(request);
        when(requestRecordService.start(anyString(), anyString(), anyString()))
                .thenReturn(new RequestRecordStartResult(false, record));

        assertThatThrownBy(() -> aspect.around(joinPoint))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(ex.getMessage()).isEqualTo("请求已处理，请勿重复提交");
                });
        verify(joinPoint, never()).proceed();
    }

    @Test
    void duplicateProcessingRequestShouldRejectAsProcessing() throws Throwable {
        TestRequest request = new TestRequest();
        request.setGlobalSerialNo("serial-002");
        RequestRecordEntity record = new RequestRecordEntity();
        record.setGlobalSerialNo("serial-002");
        record.setBusinessType("CUSTOMER_CREATE");
        record.setStatus(RequestRecordStatus.PROCESSING);

        ProceedingJoinPoint joinPoint = joinPointFor(request);
        when(requestRecordService.start(anyString(), anyString(), anyString()))
                .thenReturn(new RequestRecordStartResult(false, record));

        assertThatThrownBy(() -> aspect.around(joinPoint))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(ex.getMessage()).isEqualTo("请求正在处理中，请勿重复提交");
                });
        verify(joinPoint, never()).proceed();
    }

    private ProceedingJoinPoint joinPointFor(TestRequest request) throws NoSuchMethodException {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        TestController controller = new TestController();
        when(joinPoint.getArgs()).thenReturn(new Object[]{request});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(controller);
        when(signature.getMethod()).thenReturn(TestController.class.getMethod("create", TestRequest.class));
        when(signature.getReturnType()).thenReturn(String.class);
        return joinPoint;
    }

    static class TestRequest extends BaseRequestDTO {
    }

    static class TestController {
        @ApsIdempotent(businessType = "CUSTOMER_CREATE")
        public String create(TestRequest request) {
            return "ok";
        }
    }
}
