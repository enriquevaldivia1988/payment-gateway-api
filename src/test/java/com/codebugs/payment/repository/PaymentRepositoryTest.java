package com.codebugs.payment.repository;

import com.codebugs.payment.model.Payment;
import com.codebugs.payment.model.PaymentStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment createPayment(String merchantId, String idempotencyKey) {
        Payment payment = new Payment();
        payment.setMerchantId(merchantId);
        payment.setAmount(5000L);
        payment.setCurrency("USD");
        payment.setCardLastFour("4242");
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setIdempotencyKey(idempotencyKey);
        return paymentRepository.save(payment);
    }

    @Test
    @DisplayName("should find payment by id and merchant")
    void shouldFindByIdAndMerchant() {
        Payment saved = createPayment("merchant_001", null);

        Optional<Payment> found = paymentRepository.findByIdAndMerchantId(saved.getId(), "merchant_001");

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    @DisplayName("should not find payment for different merchant")
    void shouldNotFindForDifferentMerchant() {
        Payment saved = createPayment("merchant_001", null);

        Optional<Payment> found = paymentRepository.findByIdAndMerchantId(saved.getId(), "merchant_002");

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("should find payment by idempotency key")
    void shouldFindByIdempotencyKey() {
        createPayment("merchant_001", "key-abc-123");

        Optional<Payment> found = paymentRepository.findByIdempotencyKey("key-abc-123");

        assertTrue(found.isPresent());
        assertEquals("key-abc-123", found.get().getIdempotencyKey());
    }

    @Test
    @DisplayName("should return empty for unknown idempotency key")
    void shouldReturnEmptyForUnknownKey() {
        Optional<Payment> found = paymentRepository.findByIdempotencyKey("unknown-key");

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("should persist and retrieve all payment fields")
    void shouldPersistAllFields() {
        Payment saved = createPayment("merchant_001", "key-xyz");

        Payment found = paymentRepository.findById(saved.getId()).orElseThrow();

        assertEquals("merchant_001", found.getMerchantId());
        assertEquals(5000L, found.getAmount());
        assertEquals("USD", found.getCurrency());
        assertEquals("4242", found.getCardLastFour());
        assertEquals(PaymentStatus.AUTHORIZED, found.getStatus());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }
}
