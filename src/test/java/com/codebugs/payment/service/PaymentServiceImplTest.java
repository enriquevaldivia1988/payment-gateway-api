package com.codebugs.payment.service;

import com.codebugs.payment.exception.PaymentNotFoundException;
import com.codebugs.payment.exception.PaymentStateException;
import com.codebugs.payment.model.Payment;
import com.codebugs.payment.model.PaymentStatus;
import com.codebugs.payment.processor.CardProcessor;
import com.codebugs.payment.processor.ProcessorResult;
import com.codebugs.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CardProcessor cardProcessor;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment payment;
    private static final String MERCHANT_ID = "merchant_001";
    private static final UUID PAYMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        payment = new Payment();
        payment.setId(PAYMENT_ID);
        payment.setMerchantId(MERCHANT_ID);
        payment.setAmount(2500L);
        payment.setCurrency("USD");
        payment.setCardLastFour("4242");
    }

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {

        @Test
        @DisplayName("should authorize and persist payment when processor approves")
        void shouldAuthorizePayment() {
            when(cardProcessor.authorize(any())).thenReturn(ProcessorResult.approved("ref_123"));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Payment result = paymentService.createPayment(payment);

            assertEquals(PaymentStatus.AUTHORIZED, result.getStatus());
            assertEquals("ref_123", result.getProcessorRef());
            verify(paymentRepository).save(payment);
        }

        @Test
        @DisplayName("should mark as FAILED when processor declines")
        void shouldFailWhenDeclined() {
            when(cardProcessor.authorize(any())).thenReturn(ProcessorResult.declined("insufficient_funds"));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Payment result = paymentService.createPayment(payment);

            assertEquals(PaymentStatus.FAILED, result.getStatus());
            assertNull(result.getProcessorRef());
        }

        @Test
        @DisplayName("should return existing payment for duplicate idempotency key")
        void shouldReturnExistingForDuplicateKey() {
            payment.setIdempotencyKey("unique-key-123");
            payment.setStatus(PaymentStatus.AUTHORIZED);
            when(paymentRepository.findByIdempotencyKey("unique-key-123")).thenReturn(Optional.of(payment));

            Payment result = paymentService.createPayment(payment);

            assertEquals(payment, result);
            verify(cardProcessor, never()).authorize(any());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should proceed normally when idempotency key is null")
        void shouldProceedWhenNoIdempotencyKey() {
            when(cardProcessor.authorize(any())).thenReturn(ProcessorResult.approved("ref_456"));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Payment result = paymentService.createPayment(payment);

            assertEquals(PaymentStatus.AUTHORIZED, result.getStatus());
            verify(paymentRepository, never()).findByIdempotencyKey(any());
        }
    }

    @Nested
    @DisplayName("getPayment")
    class GetPayment {

        @Test
        @DisplayName("should return payment when found for merchant")
        void shouldReturnPayment() {
            when(paymentRepository.findByIdAndMerchantId(PAYMENT_ID, MERCHANT_ID))
                    .thenReturn(Optional.of(payment));

            Optional<Payment> result = paymentService.getPayment(PAYMENT_ID, MERCHANT_ID);

            assertTrue(result.isPresent());
            assertEquals(PAYMENT_ID, result.get().getId());
        }

        @Test
        @DisplayName("should return empty when payment not found")
        void shouldReturnEmptyWhenNotFound() {
            when(paymentRepository.findByIdAndMerchantId(PAYMENT_ID, MERCHANT_ID))
                    .thenReturn(Optional.empty());

            Optional<Payment> result = paymentService.getPayment(PAYMENT_ID, MERCHANT_ID);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("capturePayment")
    class CapturePayment {

        @Test
        @DisplayName("should capture an authorized payment")
        void shouldCaptureAuthorizedPayment() {
            payment.setStatus(PaymentStatus.AUTHORIZED);
            when(paymentRepository.findByIdAndMerchantId(PAYMENT_ID, MERCHANT_ID))
                    .thenReturn(Optional.of(payment));
            when(cardProcessor.capture(any())).thenReturn(ProcessorResult.approved("cap_ref"));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Payment result = paymentService.capturePayment(PAYMENT_ID, MERCHANT_ID);

            assertEquals(PaymentStatus.CAPTURED, result.getStatus());
            assertEquals("cap_ref", result.getProcessorRef());
        }

        @Test
        @DisplayName("should throw when payment is not in AUTHORIZED status")
        void shouldThrowWhenNotAuthorized() {
            payment.setStatus(PaymentStatus.CAPTURED);
            when(paymentRepository.findByIdAndMerchantId(PAYMENT_ID, MERCHANT_ID))
                    .thenReturn(Optional.of(payment));

            assertThrows(PaymentStateException.class,
                    () -> paymentService.capturePayment(PAYMENT_ID, MERCHANT_ID));
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenNotFound() {
            when(paymentRepository.findByIdAndMerchantId(PAYMENT_ID, MERCHANT_ID))
                    .thenReturn(Optional.empty());

            assertThrows(PaymentNotFoundException.class,
                    () -> paymentService.capturePayment(PAYMENT_ID, MERCHANT_ID));
        }
    }

    @Nested
    @DisplayName("refundPayment")
    class RefundPayment {

        @Test
        @DisplayName("should refund a captured payment")
        void shouldRefundCapturedPayment() {
            payment.setStatus(PaymentStatus.CAPTURED);
            when(paymentRepository.findByIdAndMerchantId(PAYMENT_ID, MERCHANT_ID))
                    .thenReturn(Optional.of(payment));
            when(cardProcessor.refund(any())).thenReturn(ProcessorResult.approved("ref_ref"));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Payment result = paymentService.refundPayment(PAYMENT_ID, MERCHANT_ID);

            assertEquals(PaymentStatus.REFUNDED, result.getStatus());
        }

        @Test
        @DisplayName("should throw when payment is not in CAPTURED status")
        void shouldThrowWhenNotCaptured() {
            payment.setStatus(PaymentStatus.AUTHORIZED);
            when(paymentRepository.findByIdAndMerchantId(PAYMENT_ID, MERCHANT_ID))
                    .thenReturn(Optional.of(payment));

            assertThrows(PaymentStateException.class,
                    () -> paymentService.refundPayment(PAYMENT_ID, MERCHANT_ID));
        }
    }

    @Nested
    @DisplayName("reversePayment")
    class ReversePayment {

        @Test
        @DisplayName("should reverse an authorized payment")
        void shouldReverseAuthorizedPayment() {
            payment.setStatus(PaymentStatus.AUTHORIZED);
            when(paymentRepository.findByIdAndMerchantId(PAYMENT_ID, MERCHANT_ID))
                    .thenReturn(Optional.of(payment));
            when(cardProcessor.reverse(any())).thenReturn(ProcessorResult.approved("rev_ref"));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Payment result = paymentService.reversePayment(PAYMENT_ID, MERCHANT_ID);

            assertEquals(PaymentStatus.REVERSED, result.getStatus());
        }

        @Test
        @DisplayName("should throw when payment is not in AUTHORIZED status")
        void shouldThrowWhenNotAuthorized() {
            payment.setStatus(PaymentStatus.CAPTURED);
            when(paymentRepository.findByIdAndMerchantId(PAYMENT_ID, MERCHANT_ID))
                    .thenReturn(Optional.of(payment));

            assertThrows(PaymentStateException.class,
                    () -> paymentService.reversePayment(PAYMENT_ID, MERCHANT_ID));
        }
    }
}
