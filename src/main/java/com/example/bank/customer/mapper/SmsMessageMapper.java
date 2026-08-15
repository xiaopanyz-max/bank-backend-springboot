package com.example.bank.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bank.customer.entity.SmsMessageEntity;
import org.apache.ibatis.annotations.Mapper;

/** Mapper for SMS message lifecycle records. */
@Mapper
public interface SmsMessageMapper extends BaseMapper<SmsMessageEntity> {
}
