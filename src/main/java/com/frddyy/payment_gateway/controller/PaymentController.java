package com.frddyy.payment_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frddyy.payment_gateway.dto.PaymentRequest;
import com.frddyy.payment_gateway.dto.PaymentResponse;
import com.frddyy.payment_gateway.service.JwtService;
import com.frddyy.payment_gateway.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtService jwtService;

    // Ubah constructor menjadi seperti ini
    public PaymentController(PaymentService paymentService, JwtService jwtService) {
        this.paymentService = paymentService;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Create a new payment transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest paymentRequest) {
        PaymentResponse response = paymentService.createPayment(paymentRequest);

        // Periksa status bisnis dari service
        if ("SUCCESS".equals(response.getStatus())) {
            // Jika sukses, kembalikan 201 Created
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            // Jika gagal (misal: FAILED), kembalikan 400 Bad Request
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction status by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable UUID id) {
        PaymentResponse response = paymentService.getPaymentStatus(id);
        return ResponseEntity.ok(response);
    }
}