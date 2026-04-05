--liquibase formatted sql

--changeset system:002-create-events-table
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizer_id UUID NOT NULL,
    venue_id UUID,
    title VARCHAR(500) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    cover_image_url VARCHAR(512),
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    sale_starts_at TIMESTAMPTZ,
    sale_ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_events_venue FOREIGN KEY (venue_id) REFERENCES venues(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX idx_events_organizer_slug ON events(organizer_id, slug);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_category ON events(category);
CREATE INDEX idx_events_sale_starts ON events(sale_starts_at);
CREATE INDEX idx_events_venue_id ON events(venue_id);

--rollback DROP TABLE events CASCADE;
