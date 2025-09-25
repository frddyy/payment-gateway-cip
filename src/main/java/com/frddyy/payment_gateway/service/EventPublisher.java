package com.frddyy.payment_gateway.service;

import com.frddyy.payment_gateway.dto.TransactionSuccessEvent;

public interface EventPublisher {
    void publishTransactionSuccessEvent(TransactionSuccessEvent event);
}