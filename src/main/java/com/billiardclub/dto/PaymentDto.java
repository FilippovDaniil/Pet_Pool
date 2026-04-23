package com.billiardclub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentDto {

    @NotBlank(message = "Укажите способ оплаты")
    private String paymentMethod;
}
