-- =====================================================================
-- V4__fix_order_type_column.sql  (practice-service, schema: practice)
--
-- Fixes a schema/entity mismatch: the "orders.order_type" column was
-- left as plain VARCHAR in the database (from an older/manual change),
-- but the Order.java entity expects it to be the real Postgres enum
-- type "practice.order_type". This caused Hibernate schema-validation
-- to fail on startup with:
--   "wrong column type encountered in column [order_type] ...
--    found VARCHAR, but expecting practice.order_type (Types#OTHER)"
--
-- This migration converts the existing column to the correct enum type.
-- =====================================================================

ALTER TABLE practice.orders
    ALTER COLUMN order_type TYPE practice.order_type
    USING order_type::practice.order_type;