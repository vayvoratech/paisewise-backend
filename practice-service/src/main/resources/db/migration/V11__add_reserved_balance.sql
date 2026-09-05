ALTER TABLE practice.paper_accounts
ADD COLUMN reserved_balance NUMERIC(14, 2) NOT NULL
    DEFAULT 0.00;