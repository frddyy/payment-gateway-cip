package com.frddyy.payment_gateway.service;

import com.frddyy.payment_gateway.client.BillerAggregatorClient;
import com.frddyy.payment_gateway.dto.BillerPayRequest;
import com.frddyy.payment_gateway.dto.BillerPayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BillerAggregatorServiceTest {

    private BillerAggregatorClient billerAggregatorClient;
    private BillerAggregatorService billerAggregatorService;

    @BeforeEach
    void setUp() {
        billerAggregatorClient = mock(BillerAggregatorClient.class);
        billerAggregatorService = new BillerAggregatorService(billerAggregatorClient);
    }

    @Test
    void testPay_Success() {
        // given
        BillerPayRequest request = new BillerPayRequest("ORDER123", BigDecimal.valueOf(10000), "VA");
        BillerPayResponse expectedResponse = new BillerPayResponse("REF123", "SUCCESS");

        when(billerAggregatorClient.pay(request)).thenReturn(expectedResponse);

        // when
        BillerPayResponse response = billerAggregatorService.pay(request);

        // then
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.billerReference()).isEqualTo("REF123");

        verify(billerAggregatorClient, times(1)).pay(request);
    }

    @Test
    void testPay_FallbackTriggered() {
        // given
        BillerPayRequest request = new BillerPayRequest("ORDER123", BigDecimal.valueOf(10000), "VA");

        // simulate client throws exception
        when(billerAggregatorClient.pay(request))
                .thenThrow(new RuntimeException("Service unavailable"));

        // when → call langsung fallback method
        BillerPayResponse response = billerAggregatorService.fallbackPay(
                request,
                new RuntimeException("Service unavailable")
        );

        // then
        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.billerReference()).isEqualTo("FALLBACK_REFERENCE");
    }
}
