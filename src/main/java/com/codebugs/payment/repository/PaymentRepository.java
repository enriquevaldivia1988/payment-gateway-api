package com.codebugs.payment.repository;

import com.codebugs.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndMerchantId(UUID id, String merchantId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
