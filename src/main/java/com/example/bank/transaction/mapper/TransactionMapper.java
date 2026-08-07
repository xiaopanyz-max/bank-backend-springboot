package com.example.bank.transaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bank.transaction.entity.TransactionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionMapper extends BaseMapper<TransactionEntity> {
}
