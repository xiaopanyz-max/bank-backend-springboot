package com.example.bank.customer.mapper;

import com.example.bank.common.config.MapStructSpringConfig;
import com.example.bank.customer.dto.CustomerCreateDTO;
import com.example.bank.customer.entity.CustomerEntity;
import com.example.bank.customer.vo.CustomerVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * 客户对象转换器。
 */
@Mapper(config = MapStructSpringConfig.class)
public interface CustomerMapStructMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "updater", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "accountOpenStatus", ignore = true)
    CustomerEntity toEntity(CustomerCreateDTO dto);

    CustomerVO toVO(CustomerEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "updater", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "accountOpenStatus", ignore = true)
    void copy(CustomerCreateDTO dto, @MappingTarget CustomerEntity entity);
}
