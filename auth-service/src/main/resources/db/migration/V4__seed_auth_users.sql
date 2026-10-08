-- =========================================================================
-- V4__seed_auth_users.sql (auth-service)
-- Inserts 10 test user accounts matching your platform profile ids
-- =========================================================================
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS mpin_hash VARCHAR(255);
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS kyc_status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED';
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS xp_points INTEGER NOT NULL DEFAULT 0;
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS level INTEGER NOT NULL DEFAULT 1;
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS streak_days INTEGER NOT NULL DEFAULT 0;
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS last_active_date DATE;
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

INSERT INTO auth.users (id, name, email, phone, password_hash, kyc_status, xp_points, level, streak_days, email_verified, created_at, updated_at) VALUES
                                                                                                                                                      ('00000000-0000-0000-0000-000000000001', 'Alice Smith', 'alice@example.com', '9876543211', '$2a$10$dummyHashValueForTesting1', 'VERIFIED', 1250, 2, 5, true, NOW(), NOW()),
                                                                                                                                                      ('00000000-0000-0000-0000-000000000002', 'Bob Jones', 'bob@example.com', '9876543212', '$2a$10$dummyHashValueForTesting2', 'VERIFIED', 2400, 3, 12, true, NOW(), NOW()),
                                                                                                                                                      ('00000000-0000-0000-0000-000000000003', 'Charlie Brown', 'charlie@example.com', '9876543213', '$2a$10$dummyHashValueForTesting3', 'PENDING', 450, 1, 1, false, NOW(), NOW()),
                                                                                                                                                      ('00000000-0000-0000-0000-000000000004', 'Diana Prince', 'diana@example.com', '9876543214', '$2a$10$dummyHashValueForTesting4', 'VERIFIED', 4100, 4, 25, true, NOW(), NOW()),
                                                                                                                                                      ('00000000-0000-0000-0000-000000000005', 'Evan Wright', 'evan@example.com', '9876543215', '$2a$10$dummyHashValueForTesting5', 'VERIFIED', 1100, 2, 4, true, NOW(), NOW()),
                                                                                                                                                      ('00000000-0000-0000-0000-000000000006', 'Fiona Gallagher', 'fiona@example.com', '9876543216', '$2a$10$dummyHashValueForTesting6', 'PENDING', 300, 1, 0, false, NOW(), NOW()),
                                                                                                                                                      ('00000000-0000-0000-0000-000000000007', 'George Clark', 'george@example.com', '9876543217', '$2a$10$dummyHashValueForTesting7', 'VERIFIED', 2150, 3, 9, true, NOW(), NOW()),
                                                                                                                                                      ('00000000-0000-0000-0000-000000000008', 'Hannah Abbott', 'hannah@example.com', '9876543218', '$2a$10$dummyHashValueForTesting8', 'VERIFIED', 1500, 2, 6, true, NOW(), NOW()),
                                                                                                                                                      ('00000000-0000-0000-0000-000000000009', 'Ian Malcolm', 'ian@example.com', '9876543219', '$2a$10$dummyHashValueForTesting9', 'VERIFIED', 3900, 4, 20, true, NOW(), NOW()),
                                                                                                                                                      ('00000000-0000-0000-0000-000000000010', 'Julia Roberts', 'julia@example.com', '9876543220', '$2a$10$dummyHashValueForTesting10', 'PENDING', 600, 1, 2, false, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;