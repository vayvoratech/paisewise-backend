CREATE TABLE learn.lessons (
                               id                  character varying(255) NOT NULL,
                               chapter             character varying(255),
                               chapter_no          integer NOT NULL,
                               index               integer NOT NULL,
                               jargon_words_json   character varying(1000),
                               quiz_xp             integer NOT NULL,
                               segments_json       character varying(4000),
                               title               character varying(255),
                               total               integer NOT NULL,
                               difficulty          character varying(50),       -- Added missing column
                               description         character varying(1000),     -- Added missing column
                               duration_minutes    integer,                     -- Added missing column
                               CONSTRAINT lessons_pkey PRIMARY KEY (id)
);