--liquibase formatted sql

--changeset system:001-create-inventory-snapshots-table
CREATE TABLE inventory_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    event_occurrence_id UUID NOT NULL,
    ticket_type_id UUID NOT NULL,
    total_capacity INT NOT NULL CHECK (total_capacity >= 0),
    reserved_count INT NOT NULL DEFAULT 0,
    confirmed_count INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_capacity CHECK (reserved_count + confirmed_count <= total_capacity),
    CONSTRAINT unique_occurrence_ticket_type UNIQUE (event_occurrence_id, ticket_type_id)
);

CREATE INDEX idx_inventory_event_id ON inventory_snapshots(event_id);
CREATE INDEX idx_inventory_occurrence_id ON inventory_snapshots(event_occurrence_id);

--rollback DROP TABLE inventory_snapshots CASCADE;
