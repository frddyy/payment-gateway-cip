package com.frddyy.payment_gateway.controller;

import com.frddyy.payment_gateway.dto.BillerPayRequest;
import com.frddyy.payment_gateway.dto.BillerPayResponse;
import com.frddyy.payment_gateway.dto.CoreBankDebitRequest;
import com.frddyy.payment_gateway.dto.CoreBankDebitResponse;
import com.frddyy.payment_gateway.service.BillerAggregatorService;
import com.frddyy.payment_gateway.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MockExternalApiController.class)
@AutoConfigureMockMvc(addFilters = false) // Nonaktifkan security filter agar tidak 403
class MockExternalApiControllerTest {

    @MockBean
    private JwtService jwtService;

    @MockBean
    private BillerAggregatorService billerAggregatorService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CoreBankDebitRequest debitRequest;
    private CoreBankDebitRequest failDebitRequest;

    private BillerPayRequest payRequest;
    private BillerPayRequest failPayRequest;

    @BeforeEach
    void setUp() {
        // CoreBank debit requests
        debitRequest = new CoreBankDebitRequest("1234567890", BigDecimal.valueOf(10000));
        failDebitRequest = new CoreBankDebitRequest("fail-123", BigDecimal.valueOf(10000));

        // Biller pay requests
        payRequest = new BillerPayRequest("ORDER123", BigDecimal.valueOf(5000), "VA");
        failPayRequest = new BillerPayRequest("fail-order", BigDecimal.valueOf(5000), "VA");

        // Mock BillerAggregatorService
        when(billerAggregatorService.pay(payRequest))
                .thenReturn(new BillerPayResponse("SUCCESS", "BILL123"));

        when(billerAggregatorService.pay(failPayRequest))
                .thenReturn(new BillerPayResponse("FAILED", "FALLBACK_REFERENCE"));
    }

    // ================= Core Bank Debit =================
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void mockDebit_shouldReturnSuccess() throws Exception {
        String response = mockMvc.perform(post("/mock/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debitRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CoreBankDebitResponse result = objectMapper.readValue(response, CoreBankDebitResponse.class);
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.corebankReference()).isNotNull();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void mockDebit_shouldReturnFailed() throws Exception {
        String response = mockMvc.perform(post("/mock/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failDebitRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CoreBankDebitResponse result = objectMapper.readValue(response, CoreBankDebitResponse.class);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.corebankReference()).isNull();
    }

    // ================= Biller Pay =================
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void mockPay_shouldReturnSuccess() throws Exception {
        String response = mockMvc.perform(post("/mock/biller/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BillerPayResponse result = objectMapper.readValue(response, BillerPayResponse.class);
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.billerReference()).isNotNull();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void mockPay_shouldReturnFailed() throws Exception {
        String response = mockMvc.perform(post("/mock/biller/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failPayRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BillerPayResponse result = objectMapper.readValue(response, BillerPayResponse.class);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.billerReference()).isNull();
    }

    // ================= Edge case / fallback =================
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void mockPay_shouldHandleNullRequest() throws Exception {
        String response = mockMvc.perform(post("/mock/biller/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BillerPayResponse result = objectMapper.readValue(response, BillerPayResponse.class);
        // fallback akan mengembalikan FAILED
        assertThat(result.status()).isEqualTo("FAILED");
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void mockDebit_shouldHandleNullRequest() throws Exception {
        String response = mockMvc.perform(post("/mock/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CoreBankDebitResponse result = objectMapper.readValue(response, CoreBankDebitResponse.class);
        assertThat(result.status()).isEqualTo("FAILED");
    }
}
