CREATE TABLE IF NOT EXISTS learn.jargon_terms (
    term VARCHAR(255) NOT NULL,
    definition VARCHAR(1000),
    analogy VARCHAR(1000),
    example VARCHAR(1000),
    CONSTRAINT jargon_terms_pkey PRIMARY KEY (term)
);