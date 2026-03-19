package com.codebugs.payment.controller;

import com.codebugs.payment.exception.GlobalExceptionHandler;
import com.codebugs.payment.exception.PaymentNotFoundException;
import com.codebugs.payment.exception.PaymentStateException;
import com.codebugs.payment.model.Payment;
import com.codebugs.payment.model.PaymentStatus;
import com.codebugs.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private Payment payment;
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final String MERCHANT_ID = "merchant_001";

    @BeforeEach
    void setUp() {
        payment = new Payment();
        payment.setId(PAYMENT_ID);
        payment.setMerchantId(MERCHANT_ID);
        payment.setAmount(2500L);
        payment.setCurrency("USD");
        payment.setCardLastFour("4242");
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setProcessorRef("sim_auth_abc123");
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("POST /api/v1/payments")
    class CreatePayment {

        @Test
        @DisplayName("should create payment and return 201")
        void shouldCreatePayment() throws Exception {
            when(paymentService.createPayment(any())).thenReturn(payment);

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Merchant-Id", MERCHANT_ID)
                            .header("Idempotency-Key", "key-123")
                            .content("""
                                    {"amount": 2500, "currency": "USD", "cardLastFour": "4242"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(PAYMENT_ID.toString()))
                    .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                    .andExpect(jsonPath("$.amount").value(2500));
        }

        @Test
        @DisplayName("should return 400 for invalid request body")
        void shouldReturn400ForInvalidBody() throws Exception {
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Merchant-Id", MERCHANT_ID)
                            .content("""
                                    {"amount": -1, "currency": "INVALID"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{id}")
    class GetPayment {

        @Test
        @DisplayName("should return payment when found")
        void shouldReturnPayment() throws Exception {
            when(paymentService.getPayment(PAYMENT_ID, MERCHANT_ID)).thenReturn(Optional.of(payment));

            mockMvc.perform(get("/api/v1/payments/{id}", PAYMENT_ID)
                            .header("X-Merchant-Id", MERCHANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PAYMENT_ID.toString()))
                    .andExpect(jsonPath("$.merchantId").value(MERCHANT_ID));
        }

        @Test
        @DisplayName("should return 404 when payment not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(paymentService.getPayment(PAYMENT_ID, MERCHANT_ID)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/payments/{id}", PAYMENT_ID)
                            .header("X-Merchant-Id", MERCHANT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{id}/capture")
    class CapturePayment {

        @Test
        @DisplayName("should capture payment and return 200")
        void shouldCapturePayment() throws Exception {
            payment.setStatus(PaymentStatus.CAPTURED);
            when(paymentService.capturePayment(PAYMENT_ID, MERCHANT_ID)).thenReturn(payment);

            mockMvc.perform(post("/api/v1/payments/{id}/capture", PAYMENT_ID)
                            .header("X-Merchant-Id", MERCHANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CAPTURED"));
        }

        @Test
        @DisplayName("should return 404 when payment not found")
        void shouldReturn404() throws Exception {
            when(paymentService.capturePayment(PAYMENT_ID, MERCHANT_ID))
                    .thenThrow(new PaymentNotFoundException(PAYMENT_ID));

            mockMvc.perform(post("/api/v1/payments/{id}/capture", PAYMENT_ID)
                            .header("X-Merchant-Id", MERCHANT_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 409 when payment in wrong state")
        void shouldReturn409() throws Exception {
            when(paymentService.capturePayment(PAYMENT_ID, MERCHANT_ID))
                    .thenThrow(new PaymentStateException(PaymentStatus.CAPTURED, PaymentStatus.AUTHORIZED));

            mockMvc.perform(post("/api/v1/payments/{id}/capture", PAYMENT_ID)
                            .header("X-Merchant-Id", MERCHANT_ID))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{id}/refund")
    class RefundPayment {

        @Test
        @DisplayName("should refund payment and return 200")
        void shouldRefundPayment() throws Exception {
            payment.setStatus(PaymentStatus.REFUNDED);
            when(paymentService.refundPayment(PAYMENT_ID, MERCHANT_ID)).thenReturn(payment);

            mockMvc.perform(post("/api/v1/payments/{id}/refund", PAYMENT_ID)
                            .header("X-Merchant-Id", MERCHANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REFUNDED"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{id}/reverse")
    class ReversePayment {

        @Test
        @DisplayName("should reverse payment and return 200")
        void shouldReversePayment() throws Exception {
            payment.setStatus(PaymentStatus.REVERSED);
            when(paymentService.reversePayment(PAYMENT_ID, MERCHANT_ID)).thenReturn(payment);

            mockMvc.perform(post("/api/v1/payments/{id}/reverse", PAYMENT_ID)
                            .header("X-Merchant-Id", MERCHANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REVERSED"));
        }
    }
}
