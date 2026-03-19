package com.codebugs.payment.controller;

import com.codebugs.payment.dto.PaymentRequest;
import com.codebugs.payment.dto.PaymentResponse;
import com.codebugs.payment.model.Payment;
import com.codebugs.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment processing operations")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Create a new payment", description = "Authorizes a payment against the card processor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment created and authorized"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "422", description = "Payment authorization declined")
    })
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("X-Merchant-Id") String merchantId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        log.info("Creating payment for merchant={} amount={} currency={}",
                merchantId, request.amount(), request.currency());

        Payment payment = new Payment();
        payment.setMerchantId(merchantId);
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setCardLastFour(request.cardLastFour());
        payment.setIdempotencyKey(idempotencyKey);

        Payment created = paymentService.createPayment(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve payment details", description = "Returns the current state of a payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID id,
            @RequestHeader("X-Merchant-Id") String merchantId) {
        return paymentService.getPayment(id, merchantId)
                .map(p -> ResponseEntity.ok(PaymentResponse.from(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/capture")
    @Operation(summary = "Capture an authorized payment", description = "Settles a previously authorized payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment captured"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Payment not in a capturable state")
    })
    public ResponseEntity<PaymentResponse> capturePayment(
            @Parameter(description = "Payment ID") @PathVariable UUID id,
            @RequestHeader("X-Merchant-Id") String merchantId) {
        Payment captured = paymentService.capturePayment(id, merchantId);
        return ResponseEntity.ok(PaymentResponse.from(captured));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a captured payment", description = "Issues a full refund for a captured payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment refunded"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Payment not in a refundable state")
    })
    public ResponseEntity<PaymentResponse> refundPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID id,
            @RequestHeader("X-Merchant-Id") String merchantId) {
        Payment refunded = paymentService.refundPayment(id, merchantId);
        return ResponseEntity.ok(PaymentResponse.from(refunded));
    }

    @PostMapping("/{id}/reverse")
    @Operation(summary = "Reverse an authorized payment", description = "Voids an authorization before capture")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment reversed"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Payment not in a reversible state")
    })
    public ResponseEntity<PaymentResponse> reversePayment(
            @Parameter(description = "Payment ID") @PathVariable UUID id,
            @RequestHeader("X-Merchant-Id") String merchantId) {
        Payment reversed = paymentService.reversePayment(id, merchantId);
        return ResponseEntity.ok(PaymentResponse.from(reversed));
    }
}
