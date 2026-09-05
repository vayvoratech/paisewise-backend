CREATE TABLE practice.paper_positions (
    user_id UUID NOT NULL
        REFERENCES auth.users(id),

    symbol VARCHAR(30) NOT NULL
        REFERENCES practice.symbols(symbol),

    quantity INTEGER NOT NULL
        DEFAULT 0
        CHECK (quantity >= 0),

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL
        DEFAULT NOW(),

    PRIMARY KEY (user_id, symbol)
);