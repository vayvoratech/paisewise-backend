
-- V1__baseline.sql  (profile service)

CREATE TABLE profile.badges (
                                id        uuid NOT NULL,
                                category  character varying(255),
                                emoji     character varying(255),
                                title     character varying(255),
                                user_id   character varying(255) NOT NULL,
                                CONSTRAINT badges_pkey PRIMARY KEY (id)
);

CREATE TABLE profile.profiles (
                                  user_id            character varying(255) NOT NULL,
                                  city               character varying(255),
                                  daily_reminders    boolean NOT NULL,
                                  day_streak         integer NOT NULL,
                                  handle             character varying(255),
                                  kyc_verified       boolean NOT NULL,
                                  language           character varying(255),
                                  lessons_completed  integer NOT NULL,
                                  level              integer NOT NULL,
                                  name               character varying(255),
                                  xp_total           integer NOT NULL,
                                  CONSTRAINT profiles_pkey PRIMARY KEY (user_id)
);