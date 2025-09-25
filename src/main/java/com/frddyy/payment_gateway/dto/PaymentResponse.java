package com.frddyy.payment_gateway.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    private UUID transactionId;
    private String orderId;
    private String status;
    private String corebankReference;
    private String billerReference;
    private String message;

}