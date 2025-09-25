package com.frddyy.payment_gateway.repository;

import com.frddyy.payment_gateway.model.Status;
import com.frddyy.payment_gateway.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {
    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void testSaveTransaction() {
        Transaction tx = new Transaction();
        tx.setOrderId("ORDER-1");
        tx.setAmount(BigDecimal.valueOf(1000));
        tx.setStatus(Status.SUCCESS);

        Transaction saved = transactionRepository.save(tx);

        assertThat(saved.getId()).isNotNull();
    }
}