package com.frddyy.payment_gateway.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionSuccessEvent(
        UUID transactionId,
        String orderId,
        BigDecimal amount,
        String channel,
        Instant timestamp
) {}