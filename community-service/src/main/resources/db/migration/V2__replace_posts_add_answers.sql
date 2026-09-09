-- =====================================================================
-- V2__replace_posts_add_answers.sql  (community-service, schema: community)
-- Drops:  community.posts, community.replies (old simple versions,
--         test data only per team confirmation)
-- Adds:   community.posts (rich version), community.answers
--         (replaces "replies" — matches Salmon's richer answer design)
-- Source: Salmon's final_db_schema.docx (Tables 13, 14) with fixes from
--         db_issues.docx (Issue 1: schema-qualified FK) applied.
-- =====================================================================

DROP TABLE IF EXISTS community.replies CASCADE;
DROP TABLE IF EXISTS community.posts CASCADE;

-- =====================================================================
-- TABLE 13: community_posts (created here as community.posts)
-- =====================================================================
CREATE TABLE IF NOT EXISTS community.posts (
                                 id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 user_id             UUID NOT NULL,
                                 body                TEXT NOT NULL CHECK (char_length(body) BETWEEN 10 AND 1000),
                                 language            VARCHAR(5) NOT NULL DEFAULT 'hi' CHECK (language IN ('hi','en','ta','te','mr','bn','gu','kn')),
    tags                TEXT[] NOT NULL DEFAULT '{}',
    upvote_count        INTEGER NOT NULL DEFAULT 0 CHECK (upvote_count >= 0),
    answer_count        INTEGER NOT NULL DEFAULT 0 CHECK (answer_count >= 0),
    view_count          INTEGER NOT NULL DEFAULT 0,
    is_answered         BOOLEAN NOT NULL DEFAULT false,
    accepted_answer_id  UUID,
    is_removed          BOOLEAN NOT NULL DEFAULT false,
    removed_reason      VARCHAR(100),
    removed_by          UUID,
    is_pinned           BOOLEAN NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_community_posts_feed ON community.posts(created_at DESC, language) WHERE is_removed = false;
CREATE INDEX idx_community_posts_tags ON community.posts USING gin(tags) WHERE is_removed = false;
CREATE INDEX idx_community_posts_unanswered ON community.posts(created_at DESC) WHERE is_answered = false AND is_removed = false;
CREATE INDEX idx_community_posts_user ON community.posts(user_id, created_at DESC);

-- =====================================================================
-- TABLE 14: community_answers (created here as community.answers)
-- =====================================================================
CREATE TABLE IF NOT EXISTS community.answers (
                                   id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   post_id             UUID NOT NULL REFERENCES community.posts(id) ON DELETE CASCADE,
                                   user_id             UUID NOT NULL,
                                   body                TEXT NOT NULL CHECK (char_length(body) BETWEEN 10 AND 2000),
                                   language            VARCHAR(5) NOT NULL DEFAULT 'hi',
                                   upvote_count        INTEGER NOT NULL DEFAULT 0 CHECK (upvote_count >= 0),
                                   is_verified_helper  BOOLEAN NOT NULL DEFAULT false,
                                   is_accepted         BOOLEAN NOT NULL DEFAULT false,
                                   is_removed          BOOLEAN NOT NULL DEFAULT false,
                                   removed_reason      VARCHAR(100),
                                   created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_community_answers_post ON community.answers(post_id, is_accepted DESC, upvote_count DESC) WHERE is_removed = false;
CREATE INDEX IF NOT EXISTS idx_community_answers_user ON community.answers(user_id, created_at DESC);

CREATE OR REPLACE FUNCTION community.update_post_answer_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' AND NEW.is_removed = false THEN
UPDATE community.posts SET answer_count = answer_count + 1, is_answered = true WHERE id = NEW.post_id;
ELSIF TG_OP = 'UPDATE' AND NEW.is_removed = true AND OLD.is_removed = false THEN
UPDATE community.posts SET answer_count = GREATEST(answer_count - 1, 0) WHERE id = NEW.post_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_answer_count ON community.answers;
CREATE TRIGGER trg_answer_count
    AFTER INSERT OR UPDATE ON community.answers
                        FOR EACH ROW EXECUTE FUNCTION community.update_post_answer_count();