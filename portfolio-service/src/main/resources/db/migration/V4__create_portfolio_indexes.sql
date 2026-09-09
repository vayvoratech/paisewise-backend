CREATE INDEX IF NOT EXISTS idx_holdings_user_id ON portfolio.holdings(user_id);
CREATE INDEX IF NOT EXISTS idx_holdings_symbol ON portfolio.holdings(symbol);