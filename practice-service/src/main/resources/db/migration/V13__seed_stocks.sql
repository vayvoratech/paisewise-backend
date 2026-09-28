-- V13__seed_stocks.sql
-- Seeds practice.stocks table (the price catalog for the practice screen).
-- Also ensures RELIANCE, TCS, INFY exist in practice.symbols (FK target for orders).
-- Safe to run multiple times via ON CONFLICT DO NOTHING.

INSERT INTO practice.symbols (symbol, name, exchange) VALUES
    ('RELIANCE', 'Reliance Industries Limited', 'NSE'),
    ('TCS', 'Tata Consultancy Services Limited', 'NSE'),
    ('INFY', 'Infosys Limited', 'NSE')
ON CONFLICT (symbol) DO NOTHING;

INSERT INTO practice.stocks (symbol, name, price, change_pct, emoji, trend_json) VALUES
    ('RELIANCE', 'Reliance Industries Ltd.', 2952, 1.2, '🛢️', '[2890,2905,2898,2920,2912,2935,2948,2941,2952]'),
    ('TCS', 'Tata Consultancy Services', 3801, -0.8, '💻', '[3850,3845,3838,3842,3825,3818,3810,3805,3801]'),
    ('INFY', 'Infosys Limited', 1456, -0.6, '🖥️', '[1470,1468,1472,1465,1466,1460,1458,1457,1456]')
ON CONFLICT (symbol) DO NOTHING;
