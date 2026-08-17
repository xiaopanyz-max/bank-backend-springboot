package com.example.bank.common.idempotency.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.bank.common.constants.ErrorCode;
import com.example.bank.common.exception.BusinessException;
import com.example.bank.common.idempotency.RequestRecordStartResult;
import com.example.bank.common.idempotency.RequestRecordStatus;
import com.example.bank.common.idempotency.entity.RequestRecordEntity;
import com.example.bank.common.idempotency.mapper.RequestRecordMapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Central request idempotency guard.
 *
 * <p>The caller's global serial number is written first. The database unique index decides
 * which request is the first one, so concurrent duplicate requests cannot both pass through.</p>
 */
@Service
public class RequestRecordService {

    private static final Logger log = LoggerFactory.getLogger(RequestRecordService.class);

    private final RequestRecordMapper requestRecordMapper;

    public RequestRecordService(RequestRecordMapper requestRecordMapper) {
        this.requestRecordMapper = requestRecordMapper;
    }

    public RequestRecordStartResult start(String businessType, String globalSerialNo, String requestHash) {
        RequestRecordEntity record = new RequestRecordEntity();
        record.setGlobalSerialNo(globalSerialNo);
        record.setBusinessType(businessType);
        record.setRequestHash(requestHash);
        record.setStatus(RequestRecordStatus.PROCESSING);

        try {
            requestRecordMapper.insert(record);
            log.info("request record started businessType={} globalSerialNo={}", businessType, globalSerialNo);
            return new RequestRecordStartResult(true, record);
        } catch (DuplicateKeyException ex) {
            RequestRecordEntity existing = findByGlobalSerialNo(globalSerialNo);
            if (existing == null) {
                throw ex;
            }
            assertSameRequest(businessType, requestHash, existing);
            log.info("request record idempotent hit businessType={} globalSerialNo={} status={} referenceNo={}",
                    existing.getBusinessType(), existing.getGlobalSerialNo(), existing.getStatus(), existing.getReferenceNo());
            return new RequestRecordStartResult(false, existing);
        }
    }

    public void markSuccess(String globalSerialNo, String referenceNo) {
        updateStatus(globalSerialNo, RequestRecordStatus.SUCCESS, referenceNo, null);
    }

    public void markFailed(String globalSerialNo, String failReason) {
        String safeReason = failReason == null ? null : failReason.substring(0, Math.min(500, failReason.length()));
        updateStatus(globalSerialNo, RequestRecordStatus.FAILED, null, safeReason);
    }

    public int cleanCompletedBefore(LocalDateTime threshold) {
        int deleted = requestRecordMapper.delete(new LambdaQueryWrapper<RequestRecordEntity>()
                .lt(RequestRecordEntity::getCreateTime, threshold)
                .in(RequestRecordEntity::getStatus, RequestRecordStatus.SUCCESS, RequestRecordStatus.FAILED));
        if (deleted > 0) {
            log.info("old request records cleaned count={} before={}", deleted, threshold);
        }
        return deleted;
    }

    public RequestRecordEntity findByGlobalSerialNo(String globalSerialNo) {
        return requestRecordMapper.selectOne(new LambdaQueryWrapper<RequestRecordEntity>()
                .eq(RequestRecordEntity::getGlobalSerialNo, globalSerialNo)
                .last("LIMIT 1"));
    }

    private void updateStatus(String globalSerialNo, String status, String referenceNo, String failReason) {
        requestRecordMapper.update(null, new LambdaUpdateWrapper<RequestRecordEntity>()
                .eq(RequestRecordEntity::getGlobalSerialNo, globalSerialNo)
                .set(RequestRecordEntity::getStatus, status)
                .set(referenceNo != null, RequestRecordEntity::getReferenceNo, referenceNo)
                .set(failReason != null, RequestRecordEntity::getFailReason, failReason));
        log.info("request record status changed globalSerialNo={} status={} referenceNo={}",
                globalSerialNo, status, referenceNo);
    }

    private void assertSameRequest(String businessType, String requestHash, RequestRecordEntity existing) {
        if (!existing.getBusinessType().equals(businessType) || !existing.getRequestHash().equals(requestHash)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Global serial number already exists with different request content: "
                            + existing.getGlobalSerialNo());
        }
    }
}
