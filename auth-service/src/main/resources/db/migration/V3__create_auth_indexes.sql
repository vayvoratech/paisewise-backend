-- =========================================================================
-- V3__create_auth_indexes.sql (auth-service)
-- =========================================================================
CREATE INDEX IF NOT EXISTS idx_users_email ON auth.users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON auth.users(phone);
CREATE INDEX IF NOT EXISTS idx_otp_email ON auth.otp(email);