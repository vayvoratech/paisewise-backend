-- Ensure the column exists before creating the index
ALTER TABLE practice.orders ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

-- Your index statements
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON practice.orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_symbol ON practice.orders(symbol);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON practice.orders(created_at);