package com.frddyy.payment_gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.frddyy.payment_gateway.dto.CoreBankDebitRequest;
import com.frddyy.payment_gateway.dto.CoreBankDebitResponse;

@FeignClient(
    name = "coreBankingClient",
    url = "${client.core-banking.url}" 
)
public interface CoreBankingClient {
    @PostMapping("/debit")
    CoreBankDebitResponse debit(@RequestBody CoreBankDebitRequest request);
}