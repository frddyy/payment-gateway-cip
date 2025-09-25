package com.frddyy.payment_gateway.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    void shouldStoreMessageCorrectly() {
        // given
        String errorMessage = "Transaction not found";

        // when
        ResourceNotFoundException exception = new ResourceNotFoundException(errorMessage);

        // then
        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    void shouldHaveResponseStatusNotFound() {
        ResponseStatus annotation = ResourceNotFoundException.class.getAnnotation(ResponseStatus.class);

        assertNotNull(annotation, "ResourceNotFoundException should be annotated with @ResponseStatus");
        assertEquals(HttpStatus.NOT_FOUND, annotation.value(), "Status must be 404 NOT_FOUND");
    }
}
