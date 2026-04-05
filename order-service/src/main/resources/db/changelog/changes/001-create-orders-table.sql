--liquibase formatted sql

--changeset system:001-create-orders-table
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    reservation_id UUID,
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE,
    paid_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

--rollback DROP TABLE orders CASCADE;
