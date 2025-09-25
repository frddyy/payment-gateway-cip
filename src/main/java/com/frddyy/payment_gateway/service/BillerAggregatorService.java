package com.frddyy.payment_gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.frddyy.payment_gateway.client.BillerAggregatorClient;
import com.frddyy.payment_gateway.dto.BillerPayRequest;
import com.frddyy.payment_gateway.dto.BillerPayResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class BillerAggregatorService {

    private static final Logger logger = LoggerFactory.getLogger(BillerAggregatorService.class);
    
    private final BillerAggregatorClient billerAggregatorClient;

    public BillerAggregatorService(BillerAggregatorClient billerAggregatorClient) {
        this.billerAggregatorClient = billerAggregatorClient;
    }

    @CircuitBreaker(name = "billerAggregator", fallbackMethod = "fallbackPay")
   public BillerPayResponse pay(BillerPayRequest payRequest) {
        logger.info("Calling Biller Aggregator API for orderId: {}", payRequest.orderId());
        return billerAggregatorClient.pay(payRequest);
    }

    // Fallback method when circuit breaker is open or retries exhausted
    public BillerPayResponse fallbackPay(BillerPayRequest payRequest, Exception e) {
    logger.warn("Circuit breaker fallback activated for orderId: {}. Error: {}", 
               payRequest.orderId(), e.getMessage());
    return new BillerPayResponse("FALLBACK_REFERENCE", "FAILED");
}

}