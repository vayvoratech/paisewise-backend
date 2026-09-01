-- =====================================================================
-- V2__add_user_features_and_notifications.sql  (profile-service, schema: profile)
-- Adds: user_features, notifications
-- Source: Salmon's final_db_schema.docx (Tables 17, 12).
--
-- Placement note: user_features and notifications are user-wide
-- concerns (not specific to learning, trading, or community), so they
-- live here in profile-service rather than in learn-service, matching
-- how profile.profiles already owns cross-cutting gamification fields
-- (xp_total, day_streak, level).
--
-- Must run BEFORE learn-service's V2 migration, since learn's
-- quiz_attempts trigger writes to profile.user_features.
-- =====================================================================

-- =====================================================================
-- TABLE 17: user_features
-- =====================================================================
CREATE TABLE profile.user_features (
                                       user_id                         UUID PRIMARY KEY REFERENCES auth.users(id),
                                       lessons_completed_total         INTEGER NOT NULL DEFAULT 0,
                                       lessons_completed_7d            INTEGER NOT NULL DEFAULT 0,
                                       lessons_completed_30d           INTEGER NOT NULL DEFAULT 0,
                                       quiz_attempts_total             INTEGER NOT NULL DEFAULT 0,
                                       quiz_pass_rate                  NUMERIC(5,4) NOT NULL DEFAULT 0,
                                       avg_quiz_score                  NUMERIC(5,4) NOT NULL DEFAULT 0,
                                       chapters_completed              INTEGER NOT NULL DEFAULT 0,
                                       jargon_taps_7d                  INTEGER NOT NULL DEFAULT 0,
                                       streak_days_current             INTEGER NOT NULL DEFAULT 0,
                                       streak_days_longest             INTEGER NOT NULL DEFAULT 0,
                                       sessions_7d                     INTEGER NOT NULL DEFAULT 0,
                                       sessions_30d                    INTEGER NOT NULL DEFAULT 0,
                                       avg_session_duration_secs       INTEGER NOT NULL DEFAULT 0,
                                       days_since_last_active          INTEGER NOT NULL DEFAULT 0,
                                       days_since_registration         INTEGER NOT NULL DEFAULT 0,
                                       notification_open_rate_30d      NUMERIC(5,4) NOT NULL DEFAULT 0,
                                       paper_trades_total              INTEGER NOT NULL DEFAULT 0,
                                       paper_trades_7d                 INTEGER NOT NULL DEFAULT 0,
                                       has_real_investment             BOOLEAN NOT NULL DEFAULT false,
                                       churn_score                     NUMERIC(5,4) NOT NULL DEFAULT 0.0000 CHECK (churn_score BETWEEN 0 AND 1),
                                       churn_score_computed_at         TIMESTAMPTZ,
                                       updated_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_features_churn ON profile.user_features(churn_score DESC, churn_score_computed_at)
    WHERE churn_score > 0.5;
CREATE INDEX idx_user_features_conversion_ready ON profile.user_features(paper_trades_total, has_real_investment)
    WHERE has_real_investment = false AND paper_trades_total >= 5;
CREATE INDEX idx_user_features_inactive ON profile.user_features(days_since_last_active, streak_days_current)
    WHERE days_since_last_active BETWEEN 1 AND 7;

-- =====================================================================
-- TABLE 12: notifications
-- =====================================================================
CREATE TYPE profile.notification_channel AS ENUM ('PUSH','SMS','IN_APP','EMAIL');
CREATE TYPE profile.notification_status AS ENUM ('PENDING','SENT','DELIVERED','READ','FAILED');

CREATE TABLE profile.notifications (
                                       id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       user_id         UUID NOT NULL REFERENCES auth.users(id),
                                       channel         profile.notification_channel NOT NULL,
                                       type            VARCHAR(50) NOT NULL,
                                       title           VARCHAR(200) NOT NULL,
                                       body            TEXT NOT NULL,
                                       data            JSONB NOT NULL DEFAULT '{}',
                                       status          profile.notification_status NOT NULL DEFAULT 'PENDING',
                                       fcm_message_id  VARCHAR(200),
                                       error_message   TEXT,
                                       is_read         BOOLEAN NOT NULL DEFAULT false,
                                       read_at         TIMESTAMPTZ,
                                       deep_link       VARCHAR(200),
                                       image_url       VARCHAR(500),
                                       sent_at         TIMESTAMPTZ,
                                       created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_inbox ON profile.notifications(user_id, created_at DESC) WHERE channel = 'IN_APP';
CREATE INDEX idx_notifications_unread ON profile.notifications(user_id, is_read) WHERE is_read = false AND channel = 'IN_APP';
CREATE INDEX idx_notifications_failed ON profile.notifications(status, created_at) WHERE status = 'FAILED';