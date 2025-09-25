package com.frddyy.payment_gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.frddyy.payment_gateway.dto.BillerPayRequest;
import com.frddyy.payment_gateway.dto.BillerPayResponse;

@FeignClient(name = "biller-aggregator-client", url = "${client.biller-aggregator.url}")
public interface BillerAggregatorClient {

    @PostMapping("/pay")
    BillerPayResponse pay(@RequestBody BillerPayRequest request);

}