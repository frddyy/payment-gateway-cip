package com.frddyy.payment_gateway.service;

import com.frddyy.payment_gateway.dto.PaymentRequest;
import com.frddyy.payment_gateway.dto.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPayment(PaymentRequest paymentRequest);
    PaymentResponse getPaymentStatus(UUID id);
}