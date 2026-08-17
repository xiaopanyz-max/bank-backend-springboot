package com.example.bank.common.idempotency.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bank.common.idempotency.entity.RequestRecordEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RequestRecordMapper extends BaseMapper<RequestRecordEntity> {
}
