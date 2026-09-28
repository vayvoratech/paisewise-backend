-- =====================================================================
-- V2__add_progress_and_quiz_attempts.sql  (learn-service, schema: learn)
-- Adds: user_lesson_progress, quiz_attempts
-- Alters: learn.lessons (adds xp_reward column)
-- Source: Salmon's final_db_schema.docx (Tables 3, 4) with fixes from
--         db_issues.docx (Issues 4, 5, 6, 7) applied.
--
-- IMPORTANT — cross-schema dependency: the triggers below update
-- auth.users (xp_points, level, last_active_date, streak_days) and
-- profile.user_features. Both must already exist before this file runs:
--   1. Run auth-service's V2__add_kyc_otp_audit.sql first (adds those
--      columns to auth.users).
--   2. Run profile-service's V2__add_user_features_and_notifications.sql
--      first (creates profile.user_features).
-- =====================================================================

-- Fix for Issue 5: lessons table uses quiz_xp, not xp_reward. Add the
-- expected column and backfill it from the existing quiz_xp values.
ALTER TABLE learn.lessons
    ADD COLUMN IF NOT EXISTS xp_reward INTEGER DEFAULT 50;
UPDATE learn.lessons SET xp_reward = quiz_xp WHERE xp_reward IS NULL;

-- =====================================================================
-- TABLE 3: user_lesson_progress
-- =====================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'lesson_progress_status' AND n.nspname = 'learn') THEN
        CREATE TYPE learn.lesson_progress_status AS ENUM ('NOT_STARTED','IN_PROGRESS','COMPLETED');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS learn.user_lesson_progress (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL,
    lesson_id               VARCHAR(255) NOT NULL REFERENCES learn.lessons(id), -- Issue 4 fix: VARCHAR not UUID
    status                  learn.lesson_progress_status NOT NULL DEFAULT 'NOT_STARTED',
    current_block_index     INTEGER NOT NULL DEFAULT 0,
    total_blocks            INTEGER NOT NULL DEFAULT 0,
    scroll_position_pct     NUMERIC(5,2) NOT NULL DEFAULT 0.00 CHECK (scroll_position_pct BETWEEN 0 AND 100),
    time_spent_seconds      INTEGER NOT NULL DEFAULT 0,
    jargon_taps             INTEGER NOT NULL DEFAULT 0,
    language                VARCHAR(5) NOT NULL DEFAULT 'en',
    xp_earned               INTEGER NOT NULL DEFAULT 0,
    completed_at            TIMESTAMPTZ,
    last_viewed_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_lesson_progress UNIQUE (user_id, lesson_id)
);

CREATE INDEX IF NOT EXISTS idx_user_lesson_progress_user ON learn.user_lesson_progress(user_id, status, last_viewed_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_lesson_progress_completion ON learn.user_lesson_progress(lesson_id, status, time_spent_seconds)
    WHERE status = 'COMPLETED';
CREATE INDEX IF NOT EXISTS idx_user_lesson_progress_resume ON learn.user_lesson_progress(user_id, last_viewed_at DESC)
    WHERE status = 'IN_PROGRESS';
CREATE INDEX IF NOT EXISTS idx_user_lesson_progress_today ON learn.user_lesson_progress(user_id, completed_at DESC)
    WHERE status = 'COMPLETED' AND completed_at IS NOT NULL;

CREATE OR REPLACE FUNCTION learn.award_lesson_completion_xp()
RETURNS TRIGGER AS $$
DECLARE
    lesson_xp_reward INTEGER;
BEGIN
    IF NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED' THEN
        SELECT xp_reward INTO lesson_xp_reward FROM learn.lessons WHERE id = NEW.lesson_id;  -- Issue 5 fix
        IF NEW.xp_earned = 0 AND lesson_xp_reward > 0 THEN
            NEW.xp_earned := lesson_xp_reward;
            NEW.completed_at := NOW();
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_award_lesson_xp ON learn.user_lesson_progress;
CREATE TRIGGER trg_award_lesson_xp
    BEFORE UPDATE OF status ON learn.user_lesson_progress
    FOR EACH ROW
    EXECUTE FUNCTION learn.award_lesson_completion_xp();

CREATE OR REPLACE FUNCTION learn.update_streak_on_lesson_complete()
RETURNS TRIGGER AS $$
BEGIN
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_streak_on_lesson ON learn.user_lesson_progress;
CREATE TRIGGER trg_update_streak_on_lesson
    AFTER UPDATE OF status ON learn.user_lesson_progress
    FOR EACH ROW
    EXECUTE FUNCTION learn.update_streak_on_lesson_complete();

-- =====================================================================
-- TABLE 4: quiz_attempts
-- =====================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'quiz_attempt_status' AND n.nspname = 'learn') THEN
        CREATE TYPE learn.quiz_attempt_status AS ENUM ('IN_PROGRESS','PASSED','FAILED','ABANDONED');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS learn.quiz_attempts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL,
    lesson_id               VARCHAR(255) NOT NULL REFERENCES learn.lessons(id),
    attempt_number          INTEGER NOT NULL DEFAULT 1,
    status                  learn.quiz_attempt_status NOT NULL DEFAULT 'IN_PROGRESS',
    questions_served        JSONB NOT NULL DEFAULT '[]',
    user_answers            JSONB NOT NULL DEFAULT '[]',
    total_questions         INTEGER NOT NULL DEFAULT 0,
    correct_answers         INTEGER NOT NULL DEFAULT 0,
    score_pct               NUMERIC(6,3) NOT NULL DEFAULT 0.000 CHECK (score_pct BETWEEN 0 AND 100),
    pass_threshold_pct      NUMERIC(6,3) NOT NULL DEFAULT 60.000,
    passed                  BOOLEAN NOT NULL DEFAULT false,
    xp_earned               INTEGER NOT NULL DEFAULT 0,
    xp_bonus                INTEGER NOT NULL DEFAULT 0,
    time_spent_seconds      INTEGER NOT NULL DEFAULT 0,
    started_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_at            TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    language                VARCHAR(5) NOT NULL DEFAULT 'en',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()   -- Issue 6 fix: added, was missing
);

ALTER TABLE learn.quiz_attempts ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_quiz_attempts_user_lesson ON learn.quiz_attempts(user_id, lesson_id, attempt_number DESC);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_lesson_analytics ON learn.quiz_attempts(lesson_id, passed, score_pct, created_at DESC)
    WHERE status IN ('PASSED','FAILED');
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_user_passed ON learn.quiz_attempts(user_id, passed, submitted_at DESC)
    WHERE passed = true;
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_abandoned ON learn.quiz_attempts(status, started_at DESC)
    WHERE status = 'IN_PROGRESS';
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_answers_gin ON learn.quiz_attempts USING gin(user_answers);

CREATE OR REPLACE FUNCTION learn.award_quiz_xp()
RETURNS TRIGGER AS $$
DECLARE
    base_xp INTEGER := 50;
    bonus_xp INTEGER := 0;
    total_xp INTEGER := 0;
BEGIN
    IF NEW.passed = true AND OLD.passed = false THEN
        IF NEW.score_pct = 100 THEN bonus_xp := bonus_xp + 50; END IF;
        IF NEW.attempt_number = 1 THEN bonus_xp := bonus_xp + 10; END IF;
        total_xp := base_xp + bonus_xp;

        NEW.xp_earned := base_xp;
        NEW.xp_bonus := bonus_xp;

        UPDATE auth.users
        SET xp_points = xp_points + total_xp,
            level = FLOOR((xp_points + total_xp) / 500.0) + 1,
            updated_at = NOW()
        WHERE id = NEW.user_id;

        UPDATE learn.user_lesson_progress
        SET status = 'COMPLETED', completed_at = NOW(), xp_earned = base_xp + bonus_xp
            -- fix (found during testing): xp_earned must be set to a non-zero value here,
            -- otherwise this UPDATE fires trg_award_lesson_xp on user_lesson_progress,
            -- which sees xp_earned = 0 and awards ANOTHER 50 XP for the same completion
            -- (a double-count bug not caught by Salmon's issues doc).
        WHERE user_id = NEW.user_id AND lesson_id = NEW.lesson_id AND status != 'COMPLETED';

        NEW.completed_at := NOW();
        NEW.submitted_at := COALESCE(NEW.submitted_at, NOW());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_award_quiz_xp
    BEFORE UPDATE OF passed ON learn.quiz_attempts
    FOR EACH ROW
    EXECUTE FUNCTION learn.award_quiz_xp();

CREATE OR REPLACE FUNCTION learn.mark_previous_attempts_abandoned()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.attempt_number > 1 THEN
        UPDATE learn.quiz_attempts
        SET status = 'ABANDONED'
        WHERE user_id = NEW.user_id AND lesson_id = NEW.lesson_id AND id != NEW.id AND status = 'IN_PROGRESS';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_mark_abandoned_quiz_attempts
    AFTER INSERT ON learn.quiz_attempts
    FOR EACH ROW
    EXECUTE FUNCTION learn.mark_previous_attempts_abandoned();

-- Fix for Issue 7: profile.user_features must already exist (created by
-- profile-service's own V2 migration, which must run before this one).
CREATE OR REPLACE FUNCTION learn.update_user_quiz_features()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status IN ('PASSED','FAILED') AND OLD.status = 'IN_PROGRESS' THEN
        UPDATE profile.user_features
        SET quiz_attempts_total = quiz_attempts_total + 1,
            quiz_pass_rate = (
                SELECT COUNT(*)::NUMERIC FROM learn.quiz_attempts
                WHERE user_id = NEW.user_id AND passed = true AND status = 'PASSED'
            ) / NULLIF((
                SELECT COUNT(*) FROM learn.quiz_attempts
                WHERE user_id = NEW.user_id AND status IN ('PASSED','FAILED')
            ), 0),
            avg_quiz_score = (
                SELECT AVG(score_pct) / 100.0 FROM learn.quiz_attempts
                WHERE user_id = NEW.user_id AND status IN ('PASSED','FAILED')
            ),
            updated_at = NOW()  -- fix (found during testing): table has no 'computed_at' column,
                                 -- only 'updated_at' and 'churn_score_computed_at'. Not caught by
                                 -- Salmon's issues doc (Issue 7 only addressed the missing table).
        WHERE user_id = NEW.user_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_user_quiz_features
    AFTER UPDATE OF status ON learn.quiz_attempts
    FOR EACH ROW
    EXECUTE FUNCTION learn.update_user_quiz_features();