--liquibase formatted sql

--changeset system:002-create-reservations-table
CREATE TABLE reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_occurrence_id UUID NOT NULL,
    ticket_type_id UUID NOT NULL,
    user_id UUID NOT NULL,
    order_id UUID,
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    idempotency_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_expires_at ON reservations(expires_at);
CREATE INDEX idx_reservations_user_id ON reservations(user_id);
CREATE INDEX idx_reservations_order_id ON reservations(order_id);

--rollback DROP TABLE reservations CASCADE;
