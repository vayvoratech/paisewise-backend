CREATE INDEX IF NOT EXISTS idx_posts_user_id ON community.posts(user_id);
CREATE INDEX IF NOT EXISTS idx_answers_post_id ON community.answers(post_id);
CREATE INDEX IF NOT EXISTS idx_replies_post_id ON community.replies(post_id);