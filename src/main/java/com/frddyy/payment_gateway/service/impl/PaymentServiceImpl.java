package com.frddyy.payment_gateway.service.impl;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.frddyy.payment_gateway.client.CoreBankingClient;
import com.frddyy.payment_gateway.dto.BillerPayRequest;
import com.frddyy.payment_gateway.dto.BillerPayResponse;
import com.frddyy.payment_gateway.dto.CoreBankDebitRequest;
import com.frddyy.payment_gateway.dto.CoreBankDebitResponse;
import com.frddyy.payment_gateway.dto.PaymentRequest;
import com.frddyy.payment_gateway.dto.PaymentResponse;
import com.frddyy.payment_gateway.dto.TransactionSuccessEvent;
import com.frddyy.payment_gateway.model.Channel;
import com.frddyy.payment_gateway.model.Status;
import com.frddyy.payment_gateway.model.Transaction;
import com.frddyy.payment_gateway.repository.TransactionRepository;
import com.frddyy.payment_gateway.service.BillerAggregatorService;
import com.frddyy.payment_gateway.service.PaymentService;
import com.frddyy.payment_gateway.service.EventPublisher;
import com.frddyy.payment_gateway.exception.ResourceNotFoundException;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final CoreBankingClient coreBankingClient;
    private final BillerAggregatorService billerAggregatorService; 
    private final EventPublisher eventPublisher;

    public PaymentServiceImpl(TransactionRepository transactionRepository,
                              CoreBankingClient coreBankingClient,
                              BillerAggregatorService billerAggregatorService, 
                              EventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.coreBankingClient = coreBankingClient;
        this.billerAggregatorService = billerAggregatorService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest paymentRequest) {
        logger.info("Creating new payment for orderId: {}", paymentRequest.getOrderId());

        // 1. Simpan transaksi awal dengan status PENDING
        Transaction transaction = Transaction.builder()
                .orderId(paymentRequest.getOrderId())
                .channel(Channel.valueOf(paymentRequest.getChannel().toUpperCase()))
                .amount(paymentRequest.getAmount())
                .account(paymentRequest.getAccount())
                .currency(paymentRequest.getCurrency())
                .paymentMethod(paymentRequest.getPaymentMethod())
                .status(Status.PENDING)
                .build();
        transactionRepository.save(transaction);

        try {
            // 2. Lakukan debit ke Core Banking
            logger.info("Sending debit request to Core Banking for account {}", transaction.getAccount());
            
            CoreBankDebitRequest debitRequest = new CoreBankDebitRequest(transaction.getAccount(), transaction.getAmount());
            logger.info("Calling CoreBankingClient.debit() with request: {}", debitRequest);
            
            CoreBankDebitResponse debitResponse = coreBankingClient.debit(debitRequest);
            logger.info("Received debitResponse: {}", debitResponse);

            // 3. Cek respons dari Core Banking
            if (!"SUCCESS".equals(debitResponse.status())) {
                logger.error("Debit failed from Core Banking for orderId: {}", transaction.getOrderId());
                transaction.setStatus(Status.FAILED);
                transactionRepository.save(transaction);
                return buildFailedResponse(transaction, "Debit failed from core banking.");
            }

            // Jika debit berhasil, update referensi dan simpan
            transaction.setCorebankReference(debitResponse.corebankReference());
            transactionRepository.save(transaction);
            logger.info("Debit successful for orderId: {}", transaction.getOrderId());

            // 4. Lakukan pembayaran ke Biller Aggregator MELALUI SERVICE YANG SUDAH DILINDUNGI CIRCUIT BREAKER
            logger.info("Sending payment request to Biller Aggregator for orderId {}", transaction.getOrderId());
            BillerPayRequest payRequest = new BillerPayRequest(transaction.getOrderId(), transaction.getAmount(), transaction.getPaymentMethod());
            
            // Ini sekarang sudah dilindungi Circuit Breaker dan Retry!
            BillerPayResponse payResponse = billerAggregatorService.pay(payRequest);

            logger.info("DEBUG: Biller Response Status = '{}'", payResponse.status());

            // 5. Cek respons dari Biller Aggregator
            if (!"SUCCESS".equals(payResponse.status())) {
                logger.error("Payment failed from Biller Aggregator for orderId: {}", transaction.getOrderId());
                transaction.setStatus(Status.FAILED);
                transactionRepository.save(transaction);
                return buildFailedResponse(transaction, "Payment failed at biller.");
            }

            // 6. Jika semua berhasil, update status ke SUCCESS
            logger.info("Payment successful for orderId: {}", transaction.getOrderId());
            transaction.setStatus(Status.SUCCESS);
            transaction.setBillerReference(payResponse.billerReference());
            Transaction finalTransaction = transactionRepository.save(transaction);

            // 7. Kirim event melalui Publisher
            TransactionSuccessEvent event = new TransactionSuccessEvent(
                    finalTransaction.getId(),
                    finalTransaction.getOrderId(),
                    finalTransaction.getAmount(),
                    finalTransaction.getChannel().name(),
                    Instant.now()
            );
            eventPublisher.publishTransactionSuccessEvent(event);

            return PaymentResponse.builder()
                    .transactionId(finalTransaction.getId())
                    .orderId(finalTransaction.getOrderId())
                    .status(finalTransaction.getStatus().name())
                    .corebankReference(finalTransaction.getCorebankReference())
                    .billerReference(finalTransaction.getBillerReference())
                    .build();

        } catch (Exception e) {
            logger.error("An unexpected error occurred during payment processing for orderId: {}", transaction.getOrderId(), e);
            transaction.setStatus(Status.FAILED);
            transactionRepository.save(transaction);
            return buildFailedResponse(transaction, "An unexpected error occurred.");
        }
    }

    private PaymentResponse buildFailedResponse(Transaction transaction, String message) {
        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .orderId(transaction.getOrderId())
                .status(transaction.getStatus().name())
                .message(message)
                .build();
    }

    @Override
    public PaymentResponse getPaymentStatus(UUID id) {
        logger.info("Fetching status for transaction id: {}", id);
        return transactionRepository.findById(id)
                .map(this::convertToPaymentResponse) // Jika ditemukan, konversi ke DTO
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id)); // Jika tidak, lempar exception
    }

    // Metode helper untuk mengubah Transaction (Model) menjadi PaymentResponse (DTO)
    private PaymentResponse convertToPaymentResponse(Transaction transaction) {
        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .orderId(transaction.getOrderId())
                .status(transaction.getStatus().name())
                .corebankReference(transaction.getCorebankReference())
                .billerReference(transaction.getBillerReference())
                .message(null) // Pastikan message null untuk respons sukses
                .build();
    }
}