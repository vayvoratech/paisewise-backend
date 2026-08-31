
-- V1__baseline.sql  (practice service)


CREATE TABLE practice.orders (
                                 id              uuid NOT NULL,
                                 created_at      timestamp(6) with time zone NOT NULL,
                                 order_type      character varying(255),
                                 price_per_share double precision NOT NULL,
                                 shares          integer NOT NULL,
                                 side            character varying(255),
                                 symbol          character varying(255),
                                 total_amount    double precision NOT NULL,
                                 user_id         character varying(255) NOT NULL,
                                 xp_earned       integer NOT NULL,
                                 CONSTRAINT orders_pkey PRIMARY KEY (id)
    -- NOTE: symbol is a free-text varchar here, not a FK to practice.stocks.
);

CREATE TABLE practice.stocks (
                                 symbol      character varying(255) NOT NULL,
                                 change_pct  double precision NOT NULL,
                                 emoji       character varying(255),
                                 name        character varying(255),
                                 price       double precision NOT NULL,
                                 trend_json  character varying(1000),
                                 CONSTRAINT stocks_pkey PRIMARY KEY (symbol)
);