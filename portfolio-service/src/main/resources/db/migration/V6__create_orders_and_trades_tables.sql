-- =====================================================================
-- V6__create_orders_table_and_trade_indexes.sql (portfolio-service, schema: portfolio)
-- Creates: portfolio.orders, updates portfolio.trades if needed, adds indexes
-- =====================================================================

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'order_side' AND n.nspname = 'portfolio') THEN
        CREATE TYPE portfolio.order_side AS ENUM ('BUY','SELL');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'order_type' AND n.nspname = 'portfolio') THEN
        CREATE TYPE portfolio.order_type AS ENUM ('MARKET','LIMIT','SL','SL-M');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'order_status' AND n.nspname = 'portfolio') THEN
        CREATE TYPE portfolio.order_status AS ENUM ('PENDING','OPEN','PARTIAL','COMPLETE','REJECTED','CANCELLED');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS portfolio.orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    client_order_id     VARCHAR(64) NOT NULL UNIQUE,
    symbol              VARCHAR(30) NOT NULL,
    exchange            VARCHAR(10) NOT NULL DEFAULT 'NSE',
    side                VARCHAR(10) NOT NULL,
    order_type          VARCHAR(10) NOT NULL,
    product             VARCHAR(10) NOT NULL DEFAULT 'CNC',
    quantity            INTEGER NOT NULL CHECK (quantity > 0),
    filled_qty          INTEGER NOT NULL DEFAULT 0,
    price               NUMERIC(12,2),
    trigger_price       NUMERIC(12,2),
    avg_price           NUMERIC(12,4),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    broker_order_id     VARCHAR(50),
    broker_message      TEXT,
    is_paper            BOOLEAN NOT NULL DEFAULT false,
    validity            VARCHAR(5) NOT NULL DEFAULT 'DAY',
    placed_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_portfolio_orders_user_id ON portfolio.orders(user_id, placed_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_portfolio_orders_client_order_id ON portfolio.orders(client_order_id);
CREATE INDEX IF NOT EXISTS idx_portfolio_orders_broker_order_id ON portfolio.orders(broker_order_id) WHERE broker_order_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_portfolio_orders_status ON portfolio.orders(user_id, status);

CREATE TABLE IF NOT EXISTS portfolio.trades (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID,
    user_id             UUID NOT NULL,
    symbol              VARCHAR(30) NOT NULL,
    exchange            VARCHAR(10) NOT NULL DEFAULT 'NSE',
    side                VARCHAR(10) NOT NULL,
    fill_qty            INTEGER NOT NULL CHECK (fill_qty > 0),
    fill_price          NUMERIC(12,4) NOT NULL,
    brokerage           NUMERIC(10,2) NOT NULL DEFAULT 0,
    stt                 NUMERIC(10,4) NOT NULL DEFAULT 0,
    gst                 NUMERIC(10,4) NOT NULL DEFAULT 0,
    sebi_charges        NUMERIC(10,6) NOT NULL DEFAULT 0,
    stamp_duty          NUMERIC(10,4) NOT NULL DEFAULT 0,
    total_charges       NUMERIC(10,4) NOT NULL DEFAULT 0,
    net_amount          NUMERIC(14,4) NOT NULL,
    broker_trade_id     VARCHAR(50) UNIQUE,
    is_paper            BOOLEAN NOT NULL DEFAULT false,
    traded_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_portfolio_trades_user_id ON portfolio.trades(user_id, traded_at DESC);
CREATE INDEX IF NOT EXISTS idx_portfolio_trades_order_id ON portfolio.trades(order_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_portfolio_trades_broker_trade_id ON portfolio.trades(broker_trade_id) WHERE broker_trade_id IS NOT NULL;
