package com.frddyy.payment_gateway.handler;

import com.frddyy.payment_gateway.dto.ErrorResponse;
import com.frddyy.payment_gateway.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFoundException_shouldReturn404() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("Transaction not found");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().getErrorCode());
        assertEquals("Transaction not found", response.getBody().getMessage());
    }

    @Test
    void handleGenericException_shouldReturn500() {
        // Given
        Exception ex = new Exception("Unexpected");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
        assertEquals("An unexpected error occurred. Please contact support.",
                     response.getBody().getMessage());
    }
}
