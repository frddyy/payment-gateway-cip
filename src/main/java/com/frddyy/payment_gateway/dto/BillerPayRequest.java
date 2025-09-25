package com.frddyy.payment_gateway.dto;
import java.math.BigDecimal;
public record BillerPayRequest(String orderId, BigDecimal amount, String paymentMethod) {}