--liquibase formatted sql

--changeset system:003-add-delivery-log-idempotency-index
-- Idempotency: allow only one successful (status='SENT') delivery per notification reference.
-- reference_id is tokenId parsed from the Kafka payload.
CREATE UNIQUE INDEX ux_delivery_log_sent_reference
    ON delivery_log(reference_type, reference_id, recipient)
    WHERE status = 'SENT' AND reference_id IS NOT NULL;

--rollback DROP INDEX ux_delivery_log_sent_reference;

