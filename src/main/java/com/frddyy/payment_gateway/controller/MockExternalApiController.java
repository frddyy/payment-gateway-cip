package com.frddyy.payment_gateway.controller;

import com.frddyy.payment_gateway.dto.BillerPayRequest;
import com.frddyy.payment_gateway.dto.BillerPayResponse;
import com.frddyy.payment_gateway.dto.CoreBankDebitRequest;
import com.frddyy.payment_gateway.dto.CoreBankDebitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mock")
@Profile("!prod")
public class MockExternalApiController {

    private static final Logger logger = LoggerFactory.getLogger(MockExternalApiController.class);

    @PostMapping("/corebank/debit")
    public CoreBankDebitResponse mockDebit(@RequestBody CoreBankDebitRequest request) {
        logger.info("MOCK CORE BANK: Received debit request for account {} with amount {}",
                request.account(), request.amount());

        if (request.account().toLowerCase().contains("fail")) {
            return new CoreBankDebitResponse(null, "FAILED");
        }

        return new CoreBankDebitResponse("CB-REF-" + System.currentTimeMillis(), "SUCCESS");
    }

    @PostMapping("/biller/pay")
    public BillerPayResponse mockPay(@RequestBody BillerPayRequest request) {
        logger.info("MOCK BILLER: Received payment request for orderId {} with amount {}",
                request.orderId(), request.amount());

        if (request.orderId().toLowerCase().contains("fail")) {
            return new BillerPayResponse(null, "FAILED");
        }

        return new BillerPayResponse("BILLER-REF-" + System.currentTimeMillis(), "SUCCESS");
    }
}