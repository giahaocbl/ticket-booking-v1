--liquibase formatted sql

--changeset system:003-add-version-to-orders
ALTER TABLE orders ADD COLUMN version INT NOT NULL DEFAULT 0;

--rollback ALTER TABLE orders DROP COLUMN version;
