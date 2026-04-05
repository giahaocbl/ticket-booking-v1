package com.haro.payment.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentProviderClient implements PaymentProviderClient {

    @Override
    public ChargeResult charge(String provider, BigDecimal amount, String currency, String idempotencyKey) {
        if (idempotencyKey.toLowerCase().contains("fail")) {
            return new ChargeResult(false, null, "MOCK_DECLINED", "Mock provider forced failure");
        }
        return new ChargeResult(true, "pay_" + UUID.randomUUID(), null, null);
    }

    @Override
    public RefundResult refund(String provider, String providerPaymentId, BigDecimal amount, String currency, String idempotencyKey) {
        if (idempotencyKey.toLowerCase().contains("fail")) {
            return new RefundResult(false, null, "MOCK_REFUND_FAILED", "Mock provider forced refund failure");
        }
        return new RefundResult(true, "refund_" + UUID.randomUUID(), null, null);
    }
}
