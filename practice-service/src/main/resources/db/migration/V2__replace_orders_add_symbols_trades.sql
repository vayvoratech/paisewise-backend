-- =====================================================================
-- V2__replace_orders_add_symbols_trades.sql  (practice-service, schema: practice)
-- Drops:  practice.orders (old simple version, test data only per team confirmation)
-- Adds:   practice.symbols, practice.orders (rich broker-integrated version),
--         practice.trades
-- Source: Salmon's final_db_schema.docx (Tables 5, 6) + Issue 8 fix
--         (Option A: real symbols master table).
--
-- practice.stocks (the paper-trading price catalog) is left completely
-- untouched — it serves a different purpose (display prices) from the
-- new symbols table (canonical tradable-instrument master list used as
-- an FK target for orders/holdings).
-- =====================================================================

DROP TABLE IF EXISTS practice.orders CASCADE;

-- =====================================================================
-- Fix for Issue 8 (Option A): create the symbols master table that
-- orders/holdings both reference, instead of leaving the FK dangling.
-- =====================================================================
CREATE TABLE practice.symbols (
    symbol      VARCHAR(30) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    exchange    VARCHAR(10) NOT NULL CHECK (exchange IN ('NSE','BSE'))
);

-- =====================================================================
-- TABLE 5: orders
-- =====================================================================
CREATE TYPE practice.order_side AS ENUM ('BUY','SELL');
CREATE TYPE practice.order_type AS ENUM ('MARKET','LIMIT','SL','SL-M');
CREATE TYPE practice.product_type AS ENUM ('CNC','MIS','NRML');
CREATE TYPE practice.order_status AS ENUM ('PENDING','OPEN','PARTIAL','COMPLETE','REJECTED','CANCELLED');

CREATE TABLE practice.orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES auth.users(id),   -- Issue 1 fix: schema-qualified
    client_order_id     VARCHAR(64) NOT NULL UNIQUE,
    symbol              VARCHAR(30) NOT NULL REFERENCES practice.symbols(symbol),
    exchange            VARCHAR(5) NOT NULL CHECK (exchange IN ('NSE','BSE')),
    side                practice.order_side NOT NULL,
    order_type          practice.order_type NOT NULL,
    product             practice.product_type NOT NULL,
    quantity            INTEGER NOT NULL CHECK (quantity > 0),
    filled_qty          INTEGER NOT NULL DEFAULT 0,
    price               NUMERIC(12,2),
    trigger_price       NUMERIC(12,2),
    avg_price           NUMERIC(12,4),
    status              practice.order_status NOT NULL DEFAULT 'PENDING',
    broker_order_id     VARCHAR(50),
    broker_message      TEXT,
    is_paper            BOOLEAN NOT NULL DEFAULT false,
    validity            VARCHAR(5) NOT NULL DEFAULT 'DAY' CHECK (validity IN ('DAY','IOC')),
    placed_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON practice.orders(user_id, placed_at DESC);
CREATE UNIQUE INDEX idx_orders_client_order_id ON practice.orders(client_order_id);
CREATE INDEX idx_orders_broker_order_id ON practice.orders(broker_order_id) WHERE broker_order_id IS NOT NULL;
CREATE INDEX idx_orders_open ON practice.orders(user_id, symbol) WHERE status IN ('PENDING','OPEN','PARTIAL');
CREATE INDEX idx_orders_paper_open ON practice.orders(symbol, price, side)
    WHERE is_paper = true AND status IN ('OPEN','PENDING') AND order_type = 'LIMIT';

CREATE OR REPLACE FUNCTION practice.touch_orders_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON practice.orders
    FOR EACH ROW
    EXECUTE FUNCTION practice.touch_orders_updated_at();

-- =====================================================================
-- TABLE 6: trades
-- =====================================================================
CREATE TABLE practice.trades (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID NOT NULL REFERENCES practice.orders(id),
    user_id             UUID NOT NULL REFERENCES auth.users(id),
    symbol              VARCHAR(30) NOT NULL,
    exchange            VARCHAR(5) NOT NULL,
    side                practice.order_side NOT NULL,
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
    traded_at           TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_trades_user_id ON practice.trades(user_id, traded_at DESC);
CREATE INDEX idx_trades_order_id ON practice.trades(order_id);
CREATE UNIQUE INDEX idx_trades_broker_trade_id ON practice.trades(broker_trade_id) WHERE broker_trade_id IS NOT NULL;
CREATE INDEX idx_trades_user_symbol_date ON practice.trades(user_id, symbol, traded_at) WHERE is_paper = false;