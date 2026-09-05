CREATE TABLE practice.paper_accounts (
    user_id UUID PRIMARY KEY
        REFERENCES auth.users(id),

    balance NUMERIC(14, 2) NOT NULL
        DEFAULT 100000.00,

    last_reset_at TIMESTAMPTZ NOT NULL
        DEFAULT NOW(),

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT NOW()
);