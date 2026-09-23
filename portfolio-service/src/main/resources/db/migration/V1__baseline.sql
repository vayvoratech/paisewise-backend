
-- V1__baseline.sql  (portfolio service)

CREATE TABLE portfolio.holdings (
                                    id             uuid NOT NULL,
                                    avg_price      double precision NOT NULL,
                                    current_price  double precision NOT NULL,
                                    emoji          character varying(255),
                                    name           character varying(255),
                                    note           character varying(500),
                                    shares         integer NOT NULL,
                                    symbol         character varying(255),
                                    user_id        character varying(255) NOT NULL,
                                    CONSTRAINT holdings_pkey PRIMARY KEY (id),
                                    CONSTRAINT ukkwnupuyk7uxkbv1h0jr1lnuih UNIQUE (user_id, symbol)
);