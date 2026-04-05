package com.haro.payment.controller;

import com.haro.payment.dto.CreatePaymentRequest;
import com.haro.payment.dto.CreateRefundRequest;
import com.haro.payment.dto.PaymentResponse;
import com.haro.payment.dto.RefundResponse;
import com.haro.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }
    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }
    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<RefundResponse> createRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody CreateRefundRequest request
    ) {
        RefundResponse response = paymentService.createRefund(paymentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/{paymentId}/refunds")
    public ResponseEntity<List<RefundResponse>> getRefunds(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.getRefundsByPaymentId(paymentId));
    }
}