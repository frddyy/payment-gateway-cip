package com.frddyy.payment_gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frddyy.payment_gateway.client.BillerAggregatorClient;
import com.frddyy.payment_gateway.client.CoreBankingClient;
import com.frddyy.payment_gateway.dto.BillerPayRequest;
import com.frddyy.payment_gateway.dto.BillerPayResponse;
import com.frddyy.payment_gateway.dto.CoreBankDebitRequest;
import com.frddyy.payment_gateway.dto.CoreBankDebitResponse;
import com.frddyy.payment_gateway.dto.PaymentRequest;
import com.frddyy.payment_gateway.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoreBankingClient coreBankingClient;

    @MockBean
    private BillerAggregatorClient billerAggregatorClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testuser")
    void createPayment_shouldReturn201Created_whenRequestIsValid() throws Exception {
        // Given
        PaymentRequest request = new PaymentRequest();
        request.setOrderId("INTEGRATION-TEST-002");
        request.setChannel("MOBILE_BANKING");
        request.setAmount(new BigDecimal("250000"));
        request.setAccount("1234567890");
        request.setCurrency("IDR");
        request.setPaymentMethod("VIRTUAL_ACCOUNT");

        // Stub feign client responses
        when(coreBankingClient.debit(any(CoreBankDebitRequest.class)))
                .thenReturn(new CoreBankDebitResponse("COREBANK-REF-001", "SUCCESS"));

        when(billerAggregatorClient.pay(any(BillerPayRequest.class)))
                .thenReturn(new BillerPayResponse("BILLER-REF-001", "SUCCESS"));

        // When & Then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId", is(notNullValue())))
                .andExpect(jsonPath("$.orderId", is("INTEGRATION-TEST-002")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.corebankReference", is("COREBANK-REF-001")))
                .andExpect(jsonPath("$.billerReference", is("BILLER-REF-001")));

        // DB verification
        assertEquals(1, transactionRepository.count());
    }

    @Test
    @WithMockUser(username = "testuser")
    void createPayment_shouldReturnFailed_whenCoreBankingFails() throws Exception {
        // Given
        PaymentRequest request = new PaymentRequest();
        request.setOrderId("INTEGRATION-TEST-003");
        request.setChannel("MOBILE_BANKING");
        request.setAmount(new BigDecimal("500000"));
        request.setAccount("9876543210");
        request.setCurrency("IDR");
        request.setPaymentMethod("VIRTUAL_ACCOUNT");

        // Mocking CoreBankingClient untuk balikin gagal
        CoreBankDebitResponse failedResponse = new CoreBankDebitResponse("COREBANK-REF-002", "FAILED");
        when(coreBankingClient.debit(any())).thenReturn(failedResponse);

        // When & Then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print()) // biar kelihatan di console
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.orderId", is("INTEGRATION-TEST-003")))
                .andExpect(jsonPath("$.status", is("FAILED")));

        // DB verification
        assertEquals(1, transactionRepository.count());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getPaymentStatus_shouldReturn200_whenTransactionExists() throws Exception {
        // Given: buat transaction di DB dulu
        PaymentRequest request = new PaymentRequest();
        request.setOrderId("INTEGRATION-TEST-004");
        request.setChannel("MOBILE_BANKING");
        request.setAmount(new BigDecimal("150000"));
        request.setAccount("1122334455");
        request.setCurrency("IDR");
        request.setPaymentMethod("VIRTUAL_ACCOUNT");

        // Stub feign client responses
        when(coreBankingClient.debit(any(CoreBankDebitRequest.class)))
                .thenReturn(new CoreBankDebitResponse("COREBANK-REF-003", "SUCCESS"));

        when(billerAggregatorClient.pay(any(BillerPayRequest.class)))
                .thenReturn(new BillerPayResponse("BILLER-REF-003", "SUCCESS"));

        // Simpan payment di DB via API call
        String responseContent = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Ambil transactionId dari response
        String transactionId = objectMapper.readTree(responseContent).get("transactionId").asText();

        // When & Then: panggil GET endpoint
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/payments/{id}", transactionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is(transactionId)))
                .andExpect(jsonPath("$.orderId", is("INTEGRATION-TEST-004")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.corebankReference", is("COREBANK-REF-003")))
                .andExpect(jsonPath("$.billerReference", is("BILLER-REF-003")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getPaymentStatus_shouldReturn404_whenTransactionDoesNotExist() throws Exception {
        // Generate random UUID
        String randomId = java.util.UUID.randomUUID().toString();

        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/payments/{id}", randomId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }


}
