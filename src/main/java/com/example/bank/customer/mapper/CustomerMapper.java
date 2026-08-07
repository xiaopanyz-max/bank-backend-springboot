package com.example.bank.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bank.customer.entity.CustomerEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户 Mapper。
 */
@Mapper
public interface CustomerMapper extends BaseMapper<CustomerEntity> {
}
