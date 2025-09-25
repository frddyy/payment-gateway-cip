package com.frddyy.payment_gateway.service.impl;

import com.frddyy.payment_gateway.dto.TransactionSuccessEvent;
import com.frddyy.payment_gateway.service.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventPublisher implements EventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String transactionSuccessTopic;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${application.kafka.topic.transaction-success}") String transactionSuccessTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.transactionSuccessTopic = transactionSuccessTopic;
    }

    @Override
    public void publishTransactionSuccessEvent(TransactionSuccessEvent event) {
        try {
            kafkaTemplate.send(transactionSuccessTopic, event.orderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Successfully sent transaction success event to Kafka for orderId: {}", event.orderId());
                    } else {
                        logger.error("Failed to send transaction success event to Kafka for orderId: {}", event.orderId(), ex);
                    }
                });
        } catch (Exception e) {
            logger.error("Error sending transaction success event to Kafka for orderId: {}", event.orderId(), e);
        }
    }
}