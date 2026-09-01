-- =====================================================================
-- V2__add_kyc_otp_audit.sql  (auth-service, schema: auth)
-- Adds: otp_verifications, kyc_documents, audit_log (partitioned)
-- Alters: auth.users (adds gamification + kyc_status columns needed by
--         triggers in the learn-service migration)
-- Source: Salmon's final_db_schema.docx (Tables 1, 2, 15) with all
--         fixes from db_issues.docx (Issues 1, 2, 3, 9, 10) applied.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'application_user') THEN
CREATE ROLE application_user WITH LOGIN PASSWORD 'CHANGE_ME_local_dev_only';
END IF;
END $$;

-- ---------------------------------------------------------------------
-- Fix for Issue 2: enrich auth.users with the columns every trigger in
-- this migration set (auth + learn) expects to update.
-- ---------------------------------------------------------------------
ALTER TABLE auth.users
    ADD COLUMN IF NOT EXISTS kyc_status         VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    ADD COLUMN IF NOT EXISTS xp_points          INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS level              INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS streak_days        INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_active_date   DATE;

-- =====================================================================
-- TABLE 1: otp_verifications
-- =====================================================================
CREATE TABLE auth.otp_verifications (
                                        id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        phone           VARCHAR(15) NOT NULL,
                                        otp_hash        VARCHAR(60) NOT NULL,
                                        purpose         VARCHAR(30) NOT NULL DEFAULT 'LOGIN'
                                            CHECK (purpose IN ('LOGIN','REGISTRATION','MPIN_RESET','KYC_VERIFY','WITHDRAWAL')),
                                        attempts        INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
                                        max_attempts    INTEGER NOT NULL DEFAULT 3,
                                        is_used         BOOLEAN NOT NULL DEFAULT false,
                                        is_locked       BOOLEAN NOT NULL DEFAULT false,
                                        locked_until    TIMESTAMPTZ,
                                        ip_address      VARCHAR(45),
                                        device_id       VARCHAR(200),
                                        expires_at      TIMESTAMPTZ NOT NULL,
                                        verified_at     TIMESTAMPTZ,
                                        created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_verifications_phone_purpose
    ON auth.otp_verifications(phone, purpose, created_at DESC)
    WHERE is_used = false AND is_locked = false;
CREATE INDEX idx_otp_verifications_ip
    ON auth.otp_verifications(ip_address, created_at DESC)
    WHERE ip_address IS NOT NULL;
CREATE INDEX idx_otp_verifications_expires
    ON auth.otp_verifications(expires_at)
    WHERE is_used = false;

CREATE OR REPLACE FUNCTION auth.invalidate_previous_otps()
RETURNS TRIGGER AS $$
BEGIN
UPDATE auth.otp_verifications
SET is_used = true
WHERE phone = NEW.phone
  AND purpose = NEW.purpose
  AND id != NEW.id
      AND is_used = false;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_invalidate_previous_otps
    AFTER INSERT ON auth.otp_verifications
    FOR EACH ROW
    EXECUTE FUNCTION auth.invalidate_previous_otps();

CREATE OR REPLACE FUNCTION auth.check_otp_lock()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.attempts >= NEW.max_attempts AND OLD.attempts < NEW.max_attempts THEN
        NEW.is_locked := true;
        NEW.locked_until := NOW() + INTERVAL '10 minutes';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_otp_auto_lock
    BEFORE UPDATE OF attempts ON auth.otp_verifications
    FOR EACH ROW
    EXECUTE FUNCTION auth.check_otp_lock();

-- =====================================================================
-- TABLE 2: kyc_documents
-- =====================================================================
CREATE TYPE auth.kyc_document_status AS ENUM (
    'INITIATED','PAN_SUBMITTED','DIGILOCKER_COMPLETED','VIDEO_KYC_PENDING',
    'VIDEO_KYC_COMPLETED','UNDER_REVIEW','VERIFIED','REJECTED','RESUBMISSION_REQUIRED'
);

CREATE TYPE auth.kyc_rejection_reason AS ENUM (
    'PAN_MISMATCH','NAME_MISMATCH','POOR_DOCUMENT_QUALITY','FAKE_DOCUMENT_SUSPECTED',
    'INCOMPLETE_SUBMISSION','FACE_MISMATCH','ADDRESS_MISMATCH','MINOR_DETECTED',
    'DUPLICATE_PAN','OTHER'
);

CREATE TABLE auth.kyc_documents (
                                    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    user_id                     UUID NOT NULL REFERENCES auth.users(id),   -- Issue 1 fix: schema-qualified
                                    status                      auth.kyc_document_status NOT NULL DEFAULT 'INITIATED',
                                    attempt_number              INTEGER NOT NULL DEFAULT 1,
                                    pan_encrypted               TEXT,
                                    pan_last4                   VARCHAR(4),
                                    pan_name                    VARCHAR(200),
                                    pan_dob                     DATE,
                                    pan_verified_at             TIMESTAMPTZ,
                                    aadhaar_last4                VARCHAR(4),
                                    aadhaar_name                 VARCHAR(200),
                                    aadhaar_dob                  DATE,
                                    aadhaar_gender                VARCHAR(10),
                                    aadhaar_address               JSONB,
                                    aadhaar_verified_at           TIMESTAMPTZ,
                                    digilocker_state            VARCHAR(100),
                                    digilocker_code              TEXT,
                                    digilocker_access_token      TEXT,
                                    digilocker_ref_id            VARCHAR(100),
                                    digilocker_completed_at      TIMESTAMPTZ,
                                    pan_document_s3_key          TEXT,
                                    pan_document_s3_bucket       VARCHAR(100),
                                    selfie_s3_key                TEXT,
                                    selfie_s3_bucket             VARCHAR(100),
                                    aadhaar_xml_s3_key            TEXT,
                                    video_kyc_provider           VARCHAR(50),
                                    video_kyc_ref_id             VARCHAR(100),
                                    video_kyc_url                TEXT,
                                    video_kyc_status             VARCHAR(30),
                                    video_kyc_score              NUMERIC(5,4),
                                    video_kyc_completed_at       TIMESTAMPTZ,
                                    name_match_score             NUMERIC(5,4),
                                    name_match_passed            BOOLEAN,
                                    reviewed_by                 UUID REFERENCES auth.users(id),
                                    reviewed_at                  TIMESTAMPTZ,
                                    rejection_reason             auth.kyc_rejection_reason,
                                    rejection_note               TEXT,
                                    reviewer_internal_note        TEXT,
                                    submitted_at                 TIMESTAMPTZ,
                                    verified_at                  TIMESTAMPTZ,
                                    rejected_at                  TIMESTAMPTZ,
                                    ip_address                   VARCHAR(45),
                                    device_id                    VARCHAR(200),
                                    created_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    CONSTRAINT uq_kyc_user_attempt UNIQUE (user_id, attempt_number)
);

CREATE UNIQUE INDEX idx_kyc_documents_user_latest ON auth.kyc_documents(user_id, attempt_number DESC);
CREATE INDEX idx_kyc_documents_pending_review ON auth.kyc_documents(status, submitted_at ASC)
    WHERE status IN ('VIDEO_KYC_COMPLETED','UNDER_REVIEW');
CREATE INDEX idx_kyc_documents_rejected ON auth.kyc_documents(rejection_reason, rejected_at DESC)
    WHERE status = 'REJECTED';
CREATE INDEX idx_kyc_documents_pan_last4 ON auth.kyc_documents(pan_last4) WHERE pan_last4 IS NOT NULL;
CREATE INDEX idx_kyc_documents_video_ref ON auth.kyc_documents(video_kyc_provider, video_kyc_ref_id)
    WHERE video_kyc_ref_id IS NOT NULL;

CREATE OR REPLACE FUNCTION auth.update_kyc_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_kyc_documents_updated_at
    BEFORE UPDATE ON auth.kyc_documents
    FOR EACH ROW
    EXECUTE FUNCTION auth.update_kyc_updated_at();

-- Fix for Issue 2: this trigger now works because auth.users has kyc_status (added above).
CREATE OR REPLACE FUNCTION auth.sync_kyc_status_to_user()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'VERIFIED' AND OLD.status != 'VERIFIED' THEN
UPDATE auth.users SET kyc_status = 'VERIFIED', updated_at = NOW() WHERE id = NEW.user_id;
ELSIF NEW.status = 'REJECTED' AND OLD.status != 'REJECTED' THEN
UPDATE auth.users SET kyc_status = 'REJECTED', updated_at = NOW() WHERE id = NEW.user_id;
ELSIF NEW.status = 'UNDER_REVIEW' AND OLD.status != 'UNDER_REVIEW' THEN
UPDATE auth.users SET kyc_status = 'IN_REVIEW', updated_at = NOW() WHERE id = NEW.user_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_kyc_to_user
    AFTER UPDATE OF status ON auth.kyc_documents
    FOR EACH ROW
    EXECUTE FUNCTION auth.sync_kyc_status_to_user();

-- Fix for Issue 3: single '%' placeholder, not '%%'.
CREATE OR REPLACE FUNCTION auth.prevent_full_aadhaar()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.aadhaar_last4 IS NOT NULL AND length(NEW.aadhaar_last4) != 4 THEN
        RAISE EXCEPTION 'SECURITY VIOLATION: aadhaar_last4 must be exactly 4 characters. Value length: %', length(NEW.aadhaar_last4);
END IF;
    IF NEW.aadhaar_address::TEXT ~ '\d{12}' THEN
        RAISE EXCEPTION 'SECURITY VIOLATION: Possible full Aadhaar number detected in aadhaar_address field.';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_full_aadhaar
    BEFORE INSERT OR UPDATE ON auth.kyc_documents
                         FOR EACH ROW
                         EXECUTE FUNCTION auth.prevent_full_aadhaar();

-- =====================================================================
-- TABLE 15: audit_log (fix for Issue 10: created as partitioned from the start)
-- =====================================================================
CREATE TABLE auth.audit_log (
                                id              BIGSERIAL,
                                user_id         UUID REFERENCES auth.users(id),
                                action          VARCHAR(60) NOT NULL,
                                entity_type     VARCHAR(30) NOT NULL,
                                entity_id       UUID,
                                old_values      JSONB,
                                new_values      JSONB,
                                ip_address      VARCHAR(45),
                                user_agent      TEXT,
                                device_id       VARCHAR(200),
                                session_id      VARCHAR(100),
                                request_id      VARCHAR(100),
                                result          VARCHAR(10) NOT NULL DEFAULT 'SUCCESS' CHECK (result IN ('SUCCESS','FAILURE')),
                                failure_reason  TEXT,
                                created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- One partition covering the current rollout year. Add a new one
-- (audit_log_2027 etc.) each year going forward.
CREATE TABLE auth.audit_log_2026 PARTITION OF auth.audit_log
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

CREATE INDEX idx_audit_log_user_id ON auth.audit_log(user_id, created_at DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_audit_log_failures ON auth.audit_log(action, result, created_at DESC) WHERE result = 'FAILURE';
CREATE INDEX idx_audit_log_entity ON auth.audit_log(entity_type, entity_id, created_at) WHERE entity_id IS NOT NULL;
CREATE INDEX idx_audit_log_created_at ON auth.audit_log(created_at DESC);

-- Fix for Issue 9: application_user role now exists (created at top of this file).
-- Fix (found during testing): also grant USAGE on the schema itself, or
-- application_user can't SELECT/INSERT at all — table-level grants alone
-- are not enough.
GRANT USAGE ON SCHEMA auth TO application_user;
REVOKE UPDATE, DELETE ON auth.audit_log FROM application_user;
GRANT INSERT, SELECT ON auth.audit_log TO application_user;
-- Fix (found during testing): id is BIGSERIAL, backed by a sequence —
-- INSERT fails without USAGE on it, even with INSERT granted on the table.
GRANT USAGE ON SEQUENCE auth.audit_log_id_seq TO application_user;