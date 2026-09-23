-- =====================================================================
-- V5__add_sip_webhook_events.sql  (portfolio-service, schema: portfolio)
--
-- Adds the sip_webhook_events audit table used to:
--   1. Log every Razorpay webhook event received (for debugging/replay)
--   2. Enforce idempotency — the UNIQUE constraint on event_id prevents
--      duplicate processing when Razorpay retries delivery on 5xx.
-- =====================================================================

CREATE TABLE IF NOT EXISTS portfolio.sip_webhook_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Razorpay-assigned event ID (unique per event delivery attempt).
    -- UNIQUE ensures exactly-once processing even under concurrent retries.
    event_id        VARCHAR(100) NOT NULL,
    CONSTRAINT uq_sip_webhook_event_id UNIQUE (event_id),

    -- Razorpay event type, e.g. mandate.approved, payment.captured
    event_type      VARCHAR(80)  NOT NULL,

    -- The SIP this event was resolved to (null if resolution failed)
    sip_id          UUID         REFERENCES portfolio.sips(id) ON DELETE SET NULL,

    -- Full raw JSON payload from Razorpay (for debugging and replay)
    payload         TEXT         NOT NULL,

    -- Processing outcome: OK | SKIPPED | ERROR
    status          VARCHAR(20)  NOT NULL DEFAULT 'OK'
                        CHECK (status IN ('OK', 'SKIPPED', 'ERROR')),

    -- Error detail when status = ERROR
    error_message   TEXT,

    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Index for querying events by SIP
CREATE INDEX IF NOT EXISTS idx_sip_webhook_events_sip_id
    ON portfolio.sip_webhook_events(sip_id, processed_at DESC)
    WHERE sip_id IS NOT NULL;

-- Index for monitoring failed/errored events
CREATE INDEX IF NOT EXISTS idx_sip_webhook_events_status
    ON portfolio.sip_webhook_events(status, processed_at DESC)
    WHERE status != 'OK';

-- Append-only enforcement at DB level (same pattern as portfolio.ledger)
REVOKE UPDATE, DELETE ON portfolio.sip_webhook_events FROM application_user;
GRANT INSERT, SELECT ON portfolio.sip_webhook_events TO application_user;

