--liquibase formatted sql

--changeset system:006-create-outbox-events-table
CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id UUID NOT NULL,
                               event_type VARCHAR(100) NOT NULL,
                               topic VARCHAR(255) NOT NULL,
                               payload JSONB NOT NULL,
                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_events_status ON outbox_events(status) WHERE status = 'PENDING';

--rollback DROP TABLE outbox_events CASCADE;
