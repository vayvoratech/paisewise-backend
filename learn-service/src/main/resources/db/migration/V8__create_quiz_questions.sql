CREATE TABLE IF NOT EXISTS learn.quiz_questions (
    id VARCHAR(255) NOT NULL,
    prompt VARCHAR(500),
    seconds INTEGER NOT NULL,
    xp INTEGER NOT NULL,
    order_no INTEGER NOT NULL,
    options_json VARCHAR(2000),
    explanation VARCHAR(1000),
    CONSTRAINT quiz_questions_pkey PRIMARY KEY (id)
);