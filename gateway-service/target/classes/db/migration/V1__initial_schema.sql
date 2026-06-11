-- V1__initial_schema.sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE accounts (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    balance     DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    currency    CHAR(3)       NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE transactions (
    id               UUID          PRIMARY KEY,
    sender_id        UUID          NOT NULL REFERENCES accounts(id),
    receiver_id      UUID          NOT NULL REFERENCES accounts(id),
    amount           DECIMAL(19,4) NOT NULL CHECK (amount > 0),
    currency         CHAR(3)       NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    idempotency_key  VARCHAR(255)  UNIQUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_sender    ON transactions(sender_id);
CREATE INDEX idx_transactions_receiver  ON transactions(receiver_id);
CREATE INDEX idx_transactions_status    ON transactions(status);
CREATE INDEX idx_transactions_created   ON transactions(created_at DESC);

-- Seed data for testing
INSERT INTO accounts (id, balance, currency) VALUES
    ('a0000000-0000-0000-0000-000000000001', 10000.0000, 'USD'),
    ('a0000000-0000-0000-0000-000000000002', 5000.0000,  'USD'),
    ('a0000000-0000-0000-0000-000000000003', 250.0000,   'USD');
