CREATE SCHEMA IF NOT EXISTS profile;

CREATE TABLE IF NOT EXISTS profile.badges (
    id        uuid NOT NULL,
    category  character varying(255),
    emoji     character varying(255),
    title     character varying(255),
    user_id   character varying(255) NOT NULL,
    CONSTRAINT badges_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS profile.profiles (
    user_id            character varying(255) NOT NULL,
    city               character varying(255),
    daily_reminders    boolean NOT NULL DEFAULT true,
    day_streak         integer NOT NULL DEFAULT 0,
    handle             character varying(255),
    kyc_verified       boolean NOT NULL DEFAULT false,
    language           character varying(255) DEFAULT 'en',
    lessons_completed  integer NOT NULL DEFAULT 0,
    level              integer NOT NULL DEFAULT 1,
    name               character varying(255),
    xp_total           integer NOT NULL DEFAULT 0,
    CONSTRAINT profiles_pkey PRIMARY KEY (user_id)
);

CREATE INDEX IF NOT EXISTS idx_profiles_handle ON profile.profiles(handle);
CREATE INDEX IF NOT EXISTS idx_badges_user_id ON profile.badges(user_id);