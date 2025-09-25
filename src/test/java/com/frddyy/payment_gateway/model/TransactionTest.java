package com.frddyy.payment_gateway.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

    @Test
    void builder_shouldCreateTransactionWithAllFields() {
        UUID id = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(id)
                .orderId("ORDER123")
                .channel(Channel.MOBILE_BANKING)
                .amount(BigDecimal.valueOf(10000))
                .account("1234567890")
                .currency("IDR")
                .paymentMethod("VA")
                .status(Status.SUCCESS)
                .corebankReference("CB-REF-123")
                .billerReference("BILL-REF-123")
                .build();

        assertThat(transaction.getId()).isEqualTo(id);
        assertThat(transaction.getOrderId()).isEqualTo("ORDER123");
        assertThat(transaction.getChannel()).isEqualTo(Channel.MOBILE_BANKING);
        assertThat(transaction.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(transaction.getAccount()).isEqualTo("1234567890");
        assertThat(transaction.getCurrency()).isEqualTo("IDR");
        assertThat(transaction.getPaymentMethod()).isEqualTo("VA");
        assertThat(transaction.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(transaction.getCorebankReference()).isEqualTo("CB-REF-123");
        assertThat(transaction.getBillerReference()).isEqualTo("BILL-REF-123");
    }

    @Test
    void setterAndGetter_shouldWork() {
        Transaction transaction = new Transaction();
        transaction.setOrderId("ORDER456");
        transaction.setChannel(Channel.INTERNET_BANKING);
        transaction.setAmount(BigDecimal.valueOf(5000));
        transaction.setAccount("0987654321");
        transaction.setCurrency("USD");
        transaction.setPaymentMethod("CARD");
        transaction.setStatus(Status.FAILED);
        transaction.setCorebankReference("CB-REF-456");
        transaction.setBillerReference("BILL-REF-456");

        assertThat(transaction.getOrderId()).isEqualTo("ORDER456");
        assertThat(transaction.getChannel()).isEqualTo(Channel.INTERNET_BANKING);
        assertThat(transaction.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(transaction.getAccount()).isEqualTo("0987654321");
        assertThat(transaction.getCurrency()).isEqualTo("USD");
        assertThat(transaction.getPaymentMethod()).isEqualTo("CARD");
        assertThat(transaction.getStatus()).isEqualTo(Status.FAILED);
        assertThat(transaction.getCorebankReference()).isEqualTo("CB-REF-456");
        assertThat(transaction.getBillerReference()).isEqualTo("BILL-REF-456");
    }
}
