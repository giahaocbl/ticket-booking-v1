--liquibase formatted sql

--changeset system:001-create-templates-table
CREATE TABLE templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    subject VARCHAR(500),
    body_text TEXT,
    body_html TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--rollback DROP TABLE templates CASCADE;
