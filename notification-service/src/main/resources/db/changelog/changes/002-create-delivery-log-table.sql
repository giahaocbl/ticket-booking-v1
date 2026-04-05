--liquibase formatted sql

--changeset system:002-create-delivery-log-table
CREATE TABLE delivery_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    channel VARCHAR(20) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    status VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255),
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_delivery_log_user_id ON delivery_log(user_id);
CREATE INDEX idx_delivery_log_reference ON delivery_log(reference_type, reference_id);
CREATE INDEX idx_delivery_log_created_at ON delivery_log(created_at);

--rollback DROP TABLE delivery_log CASCADE;
