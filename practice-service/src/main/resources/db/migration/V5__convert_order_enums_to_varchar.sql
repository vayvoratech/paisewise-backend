-- Converts practice.orders' enum-typed columns to VARCHAR using a temporary column mapping pattern

-- 1. Add temporary varchar columns
ALTER TABLE practice.orders ADD COLUMN side_tmp VARCHAR(10);
ALTER TABLE practice.orders ADD COLUMN order_type_tmp VARCHAR(10);
ALTER TABLE practice.orders ADD COLUMN product_tmp VARCHAR(10);
ALTER TABLE practice.orders ADD COLUMN status_tmp VARCHAR(20);

-- 2. Copy data safely casted to text
UPDATE practice.orders SET
    side_tmp = side::text,
    order_type_tmp = order_type::text,
    product_tmp = product::text,
    status_tmp = status::text;

-- 3. Drop original enum columns
ALTER TABLE practice.orders DROP COLUMN side;
ALTER TABLE practice.orders DROP COLUMN order_type;
ALTER TABLE practice.orders DROP COLUMN product;
ALTER TABLE practice.orders DROP COLUMN status;

-- 4. Rename temporary columns to original names
ALTER TABLE practice.orders RENAME COLUMN side_tmp TO side;
ALTER TABLE practice.orders RENAME COLUMN order_type_tmp TO order_type;
ALTER TABLE practice.orders RENAME COLUMN product_tmp TO product;
ALTER TABLE practice.orders RENAME COLUMN status_tmp TO status;

-- 5. Add CHECK constraints to maintain data integrity
ALTER TABLE practice.orders ADD CONSTRAINT chk_orders_side CHECK (side IN ('BUY','SELL'));
ALTER TABLE practice.orders ADD CONSTRAINT chk_orders_order_type CHECK (order_type IN ('MARKET','LIMIT','SL','SL-M'));
ALTER TABLE practice.orders ADD CONSTRAINT chk_orders_product CHECK (product IN ('CNC','MIS','NRML'));
ALTER TABLE practice.orders ADD CONSTRAINT chk_orders_status CHECK (status IN ('PENDING','OPEN','PARTIAL','COMPLETE','REJECTED','CANCELLED'));

-- 6. Drop custom enum types safely
DROP TYPE IF EXISTS practice.order_side CASCADE;
DROP TYPE IF EXISTS practice.order_type CASCADE;
DROP TYPE IF EXISTS practice.product_type CASCADE;
DROP TYPE IF EXISTS practice.order_status CASCADE;