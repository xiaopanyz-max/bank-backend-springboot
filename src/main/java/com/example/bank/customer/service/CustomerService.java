package com.example.bank.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.bank.common.page.PageResult;
import com.example.bank.customer.dto.CustomerCreateDTO;
import com.example.bank.customer.dto.CustomerPageQueryDTO;
import com.example.bank.customer.entity.CustomerEntity;
import com.example.bank.customer.vo.CustomerVO;

/**
 * 客户服务接口。
 */
public interface CustomerService extends IService<CustomerEntity> {

    Long create(CustomerCreateDTO dto);

    CustomerVO detail(Long id);

    /**
     * 逻辑删除客户记录。
     */
    void delete(Long id);

    PageResult<CustomerVO> page(CustomerPageQueryDTO query);
}
