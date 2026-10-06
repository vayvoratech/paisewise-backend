--
-- V3__add_avg_price_to_holdings.sql
ALTER TABLE portfolio.holdings
    ADD COLUMN avg_price DOUBLE PRECISION;