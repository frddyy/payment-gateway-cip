package com.frddyy.payment_gateway.service.impl;

import com.frddyy.payment_gateway.dto.TransactionSuccessEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KafkaEventPublisherTest {

    private KafkaTemplate<String, Object> kafkaTemplate;
    private KafkaEventPublisher kafkaEventPublisher;
    private final String topic = "transaction.success";

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        kafkaEventPublisher = new KafkaEventPublisher(kafkaTemplate, topic);
    }

    @Test
    void publishTransactionSuccessEvent_shouldSendEventToKafka() {
        // given
        TransactionSuccessEvent event = new TransactionSuccessEvent(
                UUID.randomUUID(),
                "ORDER123",
                BigDecimal.valueOf(10000),
                "VA",
                Instant.now()
        );
        CompletableFuture future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(eq(topic), eq(event.orderId()), eq(event))).thenReturn(future);

        // when
        kafkaEventPublisher.publishTransactionSuccessEvent(event);

        // then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate, times(1))
                .send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(topic);
        assertThat(keyCaptor.getValue()).isEqualTo(event.orderId());
        assertThat(valueCaptor.getValue()).isEqualTo(event);
    }

    @Test
    void publishTransactionSuccessEvent_whenSendThrowsException_shouldNotPropagate() {
        // given
        TransactionSuccessEvent event = new TransactionSuccessEvent(
                UUID.randomUUID(),
                "ORDER456",
                BigDecimal.valueOf(5000),
                "VA",
                Instant.now()
        );
        when(kafkaTemplate.send(eq(topic), eq(event.orderId()), eq(event)))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        // when
        kafkaEventPublisher.publishTransactionSuccessEvent(event);

        // then
        // Tidak ada exception yang dilempar keluar, verifikasi tetap memanggil send
        verify(kafkaTemplate, times(1)).send(topic, event.orderId(), event);
    }

     @Test
    void publishTransactionSuccessEvent_shouldHandleException() {
        // Mock KafkaTemplate
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);

        // Buat KafkaEventPublisher dengan topic dummy
        KafkaEventPublisher publisher = new KafkaEventPublisher(kafkaTemplate, "transaction-success-topic");

        // Mock KafkaTemplate.send agar lempar RuntimeException
        doThrow(new RuntimeException("Kafka down"))
                .when(kafkaTemplate).send(Mockito.anyString(), Mockito.any(TransactionSuccessEvent.class));

        // Buat TransactionSuccessEvent dummy
        TransactionSuccessEvent event = new TransactionSuccessEvent(
                UUID.randomUUID(),
                "ORDER-001",
                new BigDecimal("1000"),
                "MOBILE_BANKING",
                Instant.now()
        );

        // Panggil method
        publisher.publishTransactionSuccessEvent(event);

        // Tidak perlu assert, tujuan untuk men-trigger catch block & logger.error
    }
}
