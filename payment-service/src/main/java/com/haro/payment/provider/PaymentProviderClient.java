package com.haro.payment.provider;

import java.math.BigDecimal;

public interface PaymentProviderClient {

    ChargeResult charge(String provider, BigDecimal amount, String currency, String idempotencyKey);

    RefundResult refund(String provider, String providerPaymentId, BigDecimal amount, String currency, String idempotencyKey);

    record ChargeResult(
            boolean success,
            String providerPaymentId,
            String failureCode,
            String failureMessage
    ) {
    }

    record RefundResult(
            boolean success,
            String providerRefundId,
            String failureCode,
            String failureMessage
    ) {
    }
}