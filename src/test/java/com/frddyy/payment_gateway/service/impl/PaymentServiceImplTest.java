package com.frddyy.payment_gateway.service.impl;

import com.frddyy.payment_gateway.client.BillerAggregatorClient;
import com.frddyy.payment_gateway.client.CoreBankingClient;
import com.frddyy.payment_gateway.dto.*;
import com.frddyy.payment_gateway.model.Transaction;
import com.frddyy.payment_gateway.repository.TransactionRepository;
import com.frddyy.payment_gateway.service.BillerAggregatorService;
import com.frddyy.payment_gateway.service.EventPublisher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    // Membuat mock/tiruan untuk semua dependensi
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CoreBankingClient coreBankingClient;
    @Mock
    private BillerAggregatorClient billerAggregatorClient;
    @Mock
    private BillerAggregatorService billerAggregatorService;
    @Mock
    private EventPublisher eventPublisher;

    // Membuat instance dari kelas yang akan kita uji, dan inject semua mock di atas ke dalamnya
    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentRequest paymentRequest;

    // ✅ Get the actual random port used by the test server
    @LocalServerPort
    private int serverPort;

    @BeforeEach
    void setUp() {
        // Override Feign client URLs to point to the actual test server
        System.setProperty("client.core-banking.url", "http://localhost:" + serverPort);
        System.setProperty("client.biller-aggregator.url", "http://localhost:" + serverPort);
        // Menyiapkan data dummy yang akan digunakan di setiap tes
        paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId("TEST-ORDER-001");
        paymentRequest.setChannel("MOBILE_BANKING");
        paymentRequest.setAmount(new BigDecimal("10000"));
        paymentRequest.setAccount("12345");
        paymentRequest.setCurrency("IDR");
        paymentRequest.setPaymentMethod("VA");
        when(transactionRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        // Clean up system properties if needed
        System.clearProperty("client.core-banking.url");
        System.clearProperty("client.biller-aggregator.url");
    }

    @Test
    void createPayment_shouldReturnSuccess_whenAllCallsSucceed() {
        // === GIVEN (Mempersiapkan kondisi) ===

        // 1. Ajari mock repository: saat metode 'save' dipanggil, kembalikan objek transaksi yang diberikan
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Ajari mock core banking client: saat 'debit' dipanggil, kembalikan respons sukses
        when(coreBankingClient.debit(any(CoreBankDebitRequest.class)))
                .thenReturn(new CoreBankDebitResponse("CB-REF-123", "SUCCESS"));

        // 3. Ajari mock biller service: saat 'pay' dipanggil, kembalikan respons sukses
        when(billerAggregatorService.pay(any(BillerPayRequest.class)))
                .thenReturn(new BillerPayResponse("BILLER-REF-456", "SUCCESS"));

        // === WHEN (Menjalankan aksi) ===
        PaymentResponse response = paymentService.createPayment(paymentRequest);

        // === THEN (Memverifikasi hasil) ===
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("TEST-ORDER-001", response.getOrderId());
        assertEquals("CB-REF-123", response.getCorebankReference());
        assertEquals("BILLER-REF-456", response.getBillerReference());

        // Verifikasi bahwa metode 'save' dipanggil tiga kali (pertama saat PENDING, kedua saat SUCCESS)
        verify(transactionRepository, times(3)).save(any(Transaction.class));
        // Verifikasi bahwa event publisher dipanggil satu kali
        verify(eventPublisher, times(1)).publishTransactionSuccessEvent(any(TransactionSuccessEvent.class));
    }

    @Test
    void createPayment_shouldReturnFailed_whenCoreBankingDebitFails() {
        // === GIVEN (Mempersiapkan kondisi) ===

        // 1. Ajari mock core banking client untuk GAGAL
        when(coreBankingClient.debit(any(CoreBankDebitRequest.class)))
                .thenReturn(new CoreBankDebitResponse(null, "FAILED"));

        // 2. Ajari mock repository untuk tetap berfungsi seperti biasa
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // === WHEN (Menjalankan aksi) ===
        PaymentResponse response = paymentService.createPayment(paymentRequest);

        // === THEN (Memverifikasi hasil) ===
        assertNotNull(response);
        assertEquals("FAILED", response.getStatus()); // Verifikasi status FAILED
        assertNull(response.getCorebankReference());   // Verifikasi referensi null
        assertNull(response.getBillerReference());    // Verifikasi referensi null

        // Verifikasi bahwa 'save' dipanggil 2 kali (pertama saat PENDING, kedua saat FAILED)
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        
        // Verifikasi bahwa Biller Service TIDAK PERNAH dipanggil
        verify(billerAggregatorService, never()).pay(any(BillerPayRequest.class));
        
        // Verifikasi bahwa event Kafka TIDAK PERNAH dikirim
        verify(eventPublisher, never()).publishTransactionSuccessEvent(any(TransactionSuccessEvent.class));
    }

    @Test
    void createPayment_shouldReturnFailed_whenBillerAggregatorFails() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId("TEST-BILLER-FAIL");
        request.setAmount(BigDecimal.valueOf(50000));
        request.setAccount("1234567890");
        request.setCurrency("IDR");
        request.setPaymentMethod("VA");
        request.setChannel("MOBILE_BANKING");

        // CoreBanking sukses
        when(coreBankingClient.debit(any()))
                .thenReturn(new CoreBankDebitResponse("COREBANK-REF-001", "SUCCESS"));

        // Biller gagal (mock service, bukan client)
        when(billerAggregatorService.pay(any()))
                .thenReturn(new BillerPayResponse("FAILED", "FALLBACK_REF"));

        // Mock repository supaya save tidak error
        when(transactionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.createPayment(request);

        assertEquals("FAILED", response.getStatus());
        assertEquals("Payment failed at biller.", response.getMessage());

        // Verifikasi save dipanggil
        verify(transactionRepository, atLeastOnce()).save(any());
    }

    @Test
    void createPayment_shouldHandleException() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId("ORDER_EXCEPTION");
        request.setAmount(new BigDecimal("1000"));
        request.setAccount("12345");
        request.setChannel("MOBILE_BANKING");
        request.setCurrency("IDR");
        request.setPaymentMethod("VA");

        // Mock BillerAggregatorService supaya throw exception
        lenient().when(billerAggregatorService.pay(any()))
         .thenThrow(new RuntimeException("Biller service down"));

        PaymentResponse response = paymentService.createPayment(request);

        assertEquals("FAILED", response.getStatus());
        assertEquals("An unexpected error occurred.", response.getMessage());
    }

}