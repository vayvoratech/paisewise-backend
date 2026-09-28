-- V14__convert_trades_side_to_varchar.sql
--
-- Root cause: V5 ran DROP TYPE practice.order_side CASCADE, which cascade-dropped
-- the side column from practice.trades (only orders columns were manually
-- restored in V5). The trades table therefore has no side column in the live DB.
--
-- This migration adds it back as VARCHAR(10) NOT NULL with a temporary
-- default of 'BUY' to satisfy the NOT NULL constraint on any existing rows,
-- then removes the default so future inserts must supply an explicit value.

ALTER TABLE practice.trades
    ADD COLUMN side VARCHAR(10) NOT NULL DEFAULT 'BUY';

ALTER TABLE practice.trades
    ADD CONSTRAINT chk_trades_side CHECK (side IN ('BUY', 'SELL'));

ALTER TABLE practice.trades
    ALTER COLUMN side DROP DEFAULT;
