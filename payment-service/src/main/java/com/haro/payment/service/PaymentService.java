package com.haro.payment.service;

import com.haro.common.web.BadRequestException;
import com.haro.common.web.ConflictException;
import com.haro.common.web.NotFoundException;
import com.haro.payment.dto.CreatePaymentRequest;
import com.haro.payment.dto.CreateRefundRequest;
import com.haro.payment.dto.PaymentResponse;
import com.haro.payment.dto.RefundResponse;
import com.haro.payment.entity.Payment;
import com.haro.payment.entity.PaymentStatus;
import com.haro.payment.entity.Refund;
import com.haro.payment.entity.RefundStatus;
import com.haro.payment.provider.PaymentProviderClient;
import com.haro.payment.repository.PaymentRepository;
import com.haro.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentProviderClient providerClient;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Payment existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            return toPaymentResponse(existing);
        }

        Payment payment = Payment.builder()
                .orderId(request.orderId())
                .userId(request.userId())
                .amount(request.amount())
                .currency(normalizeCurrency(request.currency()))
                .status(PaymentStatus.PENDING)
                .provider(request.provider().toUpperCase(Locale.ROOT))
                .idempotencyKey(request.idempotencyKey())
                .build();

        try {
            payment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            Payment concurrent = paymentRepository.findByIdempotencyKey(request.idempotencyKey())
                    .orElseThrow(() -> new ConflictException("Payment conflicts with existing data"));
            return toPaymentResponse(concurrent);
        }

        PaymentProviderClient.ChargeResult providerResult = providerClient.charge(
                payment.getProvider(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getIdempotencyKey()
        );

        if (providerResult.success()) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setProviderPaymentId(providerResult.providerPaymentId());
            payment.setPaidAt(OffsetDateTime.now());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureCode(providerResult.failureCode());
            payment.setFailureMessage(providerResult.failureMessage());
        }

        payment = paymentRepository.save(payment);
        return toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        return toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found for order"));
        return toPaymentResponse(payment);
    }

    @Transactional
    public RefundResponse createRefund(UUID paymentId, CreateRefundRequest request) {
        Refund existing = refundRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            return toRefundResponse(existing);
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ConflictException("Only paid payments can be refunded");
        }

        String requestedCurrency = normalizeCurrency(request.currency());
        if (!payment.getCurrency().equals(requestedCurrency)) {
            throw new BadRequestException("Refund currency must match payment currency");
        }

        BigDecimal refundedAmount = refundRepository.findByPaymentIdAndStatus(paymentId, RefundStatus.SUCCEEDED).stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = payment.getAmount().subtract(refundedAmount);
        if (request.amount().compareTo(remaining) > 0) {
            throw new ConflictException("Refund amount exceeds remaining refundable amount");
        }

        Refund refund = Refund.builder()
                .payment(payment)
                .orderId(payment.getOrderId())
                .amount(request.amount())
                .currency(requestedCurrency)
                .status(RefundStatus.PENDING)
                .idempotencyKey(request.idempotencyKey())
                .build();

        try {
            refund = refundRepository.save(refund);
        } catch (DataIntegrityViolationException ex) {
            Refund concurrent = refundRepository.findByIdempotencyKey(request.idempotencyKey())
                    .orElseThrow(() -> new ConflictException("Refund conflicts with existing data"));
            return toRefundResponse(concurrent);
        }

        PaymentProviderClient.RefundResult providerResult = providerClient.refund(
                payment.getProvider(),
                payment.getProviderPaymentId(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getIdempotencyKey()
        );

        if (providerResult.success()) {
            refund.setStatus(RefundStatus.SUCCEEDED);
            refund.setProviderRefundId(providerResult.providerRefundId());
        } else {
            refund.setStatus(RefundStatus.FAILED);
        }

        refund = refundRepository.save(refund);
        return toRefundResponse(refund);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByPaymentId(UUID paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new NotFoundException("Payment not found");
        }
        return refundRepository.findByPaymentId(paymentId).stream()
                .map(this::toRefundResponse)
                .toList();
    }

    private String normalizeCurrency(String currency) {
        return currency.toUpperCase(Locale.ROOT);
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getProviderPaymentId(),
                payment.getFailureCode(),
                payment.getFailureMessage(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private RefundResponse toRefundResponse(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPayment().getId(),
                refund.getOrderId(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getStatus(),
                refund.getProviderRefundId(),
                refund.getCreatedAt(),
                refund.getUpdatedAt()
        );
    }
}

