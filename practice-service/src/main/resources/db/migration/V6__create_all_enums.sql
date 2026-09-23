DO $$ BEGIN
CREATE TYPE practice.order_side AS ENUM ('BUY', 'SELL');
CREATE TYPE practice.order_type AS ENUM ('MARKET', 'LIMIT', 'SL', 'SL-M');
CREATE TYPE practice.product_type AS ENUM ('CNC', 'MIS', 'NRML');
CREATE TYPE practice.order_status AS ENUM ('PENDING', 'OPEN', 'PARTIAL', 'COMPLETE', 'REJECTED', 'CANCELLED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;