-- V1__create_tables.sql
-- Initial schema for currency-exchange-tracker

CREATE TABLE IF NOT EXISTS exchange_rates (
    id             BIGSERIAL PRIMARY KEY,
    base_currency  VARCHAR(10)    NOT NULL,
    target_currency VARCHAR(10)   NOT NULL,
    rate           NUMERIC(20, 8) NOT NULL,
    fetched_at     TIMESTAMP      NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_exchange_rate_base
    ON exchange_rates (base_currency);

CREATE INDEX IF NOT EXISTS idx_exchange_rate_pair
    ON exchange_rates (base_currency, target_currency);

CREATE TABLE IF NOT EXISTS rate_snapshots (
    id              BIGSERIAL PRIMARY KEY,
    base_currency   VARCHAR(10)    NOT NULL,
    target_currency VARCHAR(10)    NOT NULL,
    rate            NUMERIC(20, 8) NOT NULL,
    snapshot_date   DATE           NOT NULL,
    CONSTRAINT uq_snapshot_pair_date UNIQUE (base_currency, target_currency, snapshot_date)
);

CREATE INDEX IF NOT EXISTS idx_snapshot_base
    ON rate_snapshots (base_currency);

CREATE INDEX IF NOT EXISTS idx_snapshot_pair_date
    ON rate_snapshots (base_currency, target_currency, snapshot_date);
