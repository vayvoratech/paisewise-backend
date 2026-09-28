CREATE INDEX IF NOT EXISTS idx_profiles_handle ON profile.profiles(handle);
CREATE INDEX IF NOT EXISTS idx_badges_user_id ON profile.badges(user_id);