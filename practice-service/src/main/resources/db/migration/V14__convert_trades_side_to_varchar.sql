-- V14__convert_trades_side_to_varchar.sql

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'practice' AND table_name = 'trades' AND column_name = 'side'
    ) THEN
        ALTER TABLE practice.trades ADD COLUMN side VARCHAR(10) NOT NULL DEFAULT 'BUY';
        ALTER TABLE practice.trades ALTER COLUMN side DROP DEFAULT;
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_trades_side'
    ) THEN
        ALTER TABLE practice.trades ADD CONSTRAINT chk_trades_side CHECK (side IN ('BUY', 'SELL'));
    END IF;
END $$;

