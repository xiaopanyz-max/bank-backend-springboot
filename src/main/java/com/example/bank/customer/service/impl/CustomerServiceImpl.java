package com.example.bank.customer.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bank.common.constants.ErrorCode;
import com.example.bank.common.exception.BusinessException;
import com.example.bank.common.page.PageResult;
import com.example.bank.customer.dto.CustomerCreateDTO;
import com.example.bank.customer.dto.CustomerPageQueryDTO;
import com.example.bank.customer.entity.CustomerEntity;
import com.example.bank.customer.mapper.CustomerMapStructMapper;
import com.example.bank.customer.mapper.CustomerMapper;
import com.example.bank.customer.service.CustomerService;
import com.example.bank.customer.vo.CustomerVO;
import com.example.bank.customer.outbox.CustomerOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customer service implementation. */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, CustomerEntity> implements CustomerService {

    private final CustomerMapStructMapper customerMapStructMapper;
    private final CustomerOutboxService customerOutboxService;

    public CustomerServiceImpl(CustomerMapStructMapper customerMapStructMapper,
                               CustomerOutboxService customerOutboxService) {
        this.customerMapStructMapper = customerMapStructMapper;
        this.customerOutboxService = customerOutboxService;
    }

    @Override
    @Transactional
    public Long create(CustomerCreateDTO dto) {
        boolean customerNoExists = lambdaQuery()
                .eq(CustomerEntity::getCustomerNo, dto.getCustomerNo())
                .exists();
        if (customerNoExists) {
            throw new BusinessException(ErrorCode.CONFLICT, "客户号已存在，请更换客户号后重试");
        }
        CustomerEntity entity = customerMapStructMapper.toEntity(dto);
        entity.setStatus(1);
        entity.setAccountOpenStatus("PENDING");
        save(entity);
        customerOutboxService.recordAccountOpenRequested(entity.getId(), entity.getCustomerNo());
        return entity.getId();
    }

    @Override
    public CustomerVO detail(Long id) {
        CustomerEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Customer not found");
        }
        return customerMapStructMapper.toVO(entity);
    }

    @Override
    public void delete(Long id) {
        CustomerEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Customer not found");
        }
        removeById(entity.getId());
    }

    @Override
    public PageResult<CustomerVO> page(CustomerPageQueryDTO query) {
        Page<CustomerEntity> page = lambdaQuery()
                .like(query.getKeyword() != null && !query.getKeyword().isBlank(), CustomerEntity::getName, query.getKeyword())
                .page(new Page<>(query.getPageNo(), query.getPageSize()));
        return new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages(),
                page.getRecords().stream()
                        .map(customerMapStructMapper::toVO)
                        .toList()
        );
    }
}
