package com.frddyy.payment_gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotBlank(message = "Order ID tidak boleh kosong")
    private String orderId;

    @NotBlank(message = "Channel tidak boleh kosong")
    private String channel;

    @NotNull(message = "Amount tidak boleh null")
    @Positive(message = "Amount harus bernilai positif")
    private BigDecimal amount;

    @NotBlank(message = "Currency tidak boleh kosong")
    private String currency;

    @NotBlank(message = "Payment Method tidak boleh kosong")
    private String paymentMethod;

    @NotBlank(message = "Account tidak boleh kosong")
    private String account;
}