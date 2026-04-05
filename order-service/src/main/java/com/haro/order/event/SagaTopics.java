package com.haro.order.event;


public final class SagaTopics {

    public static final String ORDER = "saga.order";
    public static final String PAYMENT = "saga.payment";
    public static final String ORDER_STATUS = "saga.order.status";

    private SagaTopics() {}
}
