--liquibase formatted sql

--changeset system:005-create-email-verification-tokens-table
CREATE TABLE email_verification_tokens (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           user_id UUID NOT NULL,
                                           token_hash VARCHAR(255) NOT NULL,
                                           expires_at TIMESTAMPTZ NOT NULL,
                                           verified_at TIMESTAMPTZ,
                                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_expires_at ON email_verification_tokens(expires_at);

--rollback DROP TABLE email_verification_tokens CASCADE;
