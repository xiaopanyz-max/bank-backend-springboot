package com.example.bank.customer.controller;

import com.example.bank.common.page.PageResult;
import com.example.bank.common.idempotency.annotation.ApsIdempotent;
import com.example.bank.common.result.Result;
import com.example.bank.customer.dto.CustomerCreateDTO;
import com.example.bank.customer.dto.CustomerPageQueryDTO;
import com.example.bank.customer.service.CustomerService;
import com.example.bank.customer.service.impl.CustomerCreateIdempotentResultResolver;
import com.example.bank.customer.vo.CustomerVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Customer APIs. */
@Validated
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @ApsIdempotent(businessType = CustomerCreateIdempotentResultResolver.BUSINESS_TYPE)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CustomerCreateDTO dto) {
        return Result.success(customerService.create(dto));
    }

    @GetMapping("/{id}")
    public Result<CustomerVO> detail(@PathVariable("id") Long id) {
        return Result.success(customerService.detail(id));
    }

    /** Physically deletes the customer row. */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        customerService.delete(id);
        return Result.success();
    }

    @PostMapping("/page")
    public Result<PageResult<CustomerVO>> page(@Valid @RequestBody CustomerPageQueryDTO query) {
        return Result.success(customerService.page(query));
    }
}
