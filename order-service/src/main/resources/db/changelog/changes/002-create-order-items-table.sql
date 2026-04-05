--liquibase formatted sql

--changeset system:002-create-order-items-table
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    event_id UUID NOT NULL,
    event_occurrence_id UUID NOT NULL,
    ticket_type_id UUID NOT NULL,
    event_title VARCHAR(500) NOT NULL,
    occurrence_starts_at TIMESTAMPTZ NOT NULL,
    ticket_type_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(19,4) NOT NULL,
    total_price DECIMAL(19,4) NOT NULL,
    reservation_id UUID,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_event_id ON order_items(event_id);
CREATE INDEX idx_order_items_occurrence ON order_items(event_occurrence_id);

--rollback DROP TABLE order_items CASCADE;
