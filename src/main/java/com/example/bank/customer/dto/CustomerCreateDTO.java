package com.example.bank.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 客户新增入参。
 */
@Data
public class CustomerCreateDTO {

    @NotBlank(message = "客户编号不能为空")
    private String customerNo;

    @NotBlank(message = "客户名称不能为空")
    private String name;

    @Pattern(regexp = "^[0-9]{11}$", message = "手机号必须是 11 位数字")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;
}
