
-- =====================================================================
-- V2__replace_holdings_add_ledger_mf_insights.sql  (portfolio-service, schema: portfolio)
-- Drops:  portfolio.holdings (old simple version, test data only per team confirmation)
-- Adds:   portfolio.holdings (rich version), portfolio.ledger,
--         portfolio.mf_schemes, portfolio.mf_investments, portfolio.sips,
--         portfolio.portfolio_insights
--
-- Cross-schema note: holdings references practice.symbols(symbol) —
-- practice-service already owns the canonical tradable-instrument
-- master list (created in its own V2 migration), so portfolio reuses
-- it rather than duplicating a second symbols table. Run
-- practice-service's V2 migration BEFORE this one.
-- =====================================================================

DROP TABLE IF EXISTS portfolio.holdings CASCADE;

-- =====================================================================
-- TABLE 8: holdings
-- =====================================================================
-- Reuse practice-service's enum types where they overlap conceptually,
-- but portfolio owns its own copy since Postgres enum types cannot be
-- shared cleanly across schemas without cross-schema type references
-- complicating every downstream service. Defined locally instead.
CREATE TYPE portfolio.product_type AS ENUM ('CNC','MIS','NRML');

CREATE TABLE IF NOT EXISTS portfolio.holdings (
                                    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    user_id             UUID NOT NULL,
                                    symbol              VARCHAR(30) NOT NULL,
                                    quantity            INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
                                    avg_cost            NUMERIC(12,4) NOT NULL,
                                    total_invested       NUMERIC(14,2) NOT NULL,
                                    product             portfolio.product_type NOT NULL DEFAULT 'CNC',
                                    is_paper            BOOLEAN NOT NULL DEFAULT false,
                                    first_bought_at      TIMESTAMPTZ NOT NULL,
                                    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    CONSTRAINT uq_holdings_user_symbol UNIQUE (user_id, symbol, product, is_paper)
);

CREATE INDEX idx_holdings_user_id ON portfolio.holdings(user_id, is_paper) WHERE quantity > 0;

-- =====================================================================
-- TABLE 7: ledger  (append-only — see Rule 1/2/3 in comments, enforced
-- at the application layer; the REVOKE below enforces Rule 1/2 at the
-- database layer too, same pattern as auth.audit_log)
-- =====================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'transaction_type' AND n.nspname = 'portfolio') THEN
        CREATE TYPE portfolio.transaction_type AS ENUM ('CREDIT','DEBIT');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS portfolio.ledger (
                                  id              BIGSERIAL PRIMARY KEY,
                                  user_id         UUID NOT NULL,
                                  type            portfolio.transaction_type NOT NULL,
                                  amount          NUMERIC(14,2) NOT NULL CHECK (amount > 0),
                                  balance_after   NUMERIC(14,2) NOT NULL,
                                  description     VARCHAR(200) NOT NULL,
                                  ref_type        VARCHAR(30),
                                  ref_id          UUID,
                                  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ledger_user_id ON portfolio.ledger(user_id, id DESC);
CREATE INDEX IF NOT EXISTS idx_ledger_ref ON portfolio.ledger(ref_type, ref_id) WHERE ref_id IS NOT NULL;

GRANT USAGE ON SCHEMA portfolio TO application_user;
REVOKE UPDATE, DELETE ON portfolio.ledger FROM application_user;
GRANT INSERT, SELECT ON portfolio.ledger TO application_user;
GRANT USAGE ON SEQUENCE portfolio.ledger_id_seq TO application_user;

-- =====================================================================
-- TABLE 9: mf_schemes
-- =====================================================================
CREATE TABLE IF NOT EXISTS portfolio.mf_schemes (
                                      scheme_code             VARCHAR(20) PRIMARY KEY,
                                      isin                    VARCHAR(12) UNIQUE,
                                      scheme_name             VARCHAR(300) NOT NULL,
                                      amc_name                VARCHAR(100) NOT NULL,
                                      amc_code                VARCHAR(20),
                                      category                VARCHAR(50) NOT NULL,
                                      sub_category            VARCHAR(50),
                                      scheme_type             VARCHAR(20) NOT NULL CHECK (scheme_type IN ('Open Ended','Close Ended','Interval')),
                                      risk_level              VARCHAR(20) NOT NULL CHECK (risk_level IN ('Low','Low to Moderate','Moderate','Moderately High','High','Very High')),
                                      nav                     NUMERIC(12,4),
                                      nav_date                DATE,
                                      min_sip_amount          NUMERIC(10,2) NOT NULL DEFAULT 100,
                                      min_lumpsum             NUMERIC(10,2) NOT NULL DEFAULT 1000,
                                      sip_multiplier          NUMERIC(10,2) NOT NULL DEFAULT 1,
                                      returns_1y              NUMERIC(8,4),
                                      returns_3y              NUMERIC(8,4),
                                      returns_5y              NUMERIC(8,4),
                                      returns_since_launch    NUMERIC(8,4),
                                      benchmark_name          VARCHAR(100),
                                      benchmark_returns_1y    NUMERIC(8,4),
                                      expense_ratio           NUMERIC(5,4),
                                      fund_manager            VARCHAR(200),
                                      fund_size_cr            NUMERIC(14,2),
                                      launch_date             DATE,
                                      is_active               BOOLEAN NOT NULL DEFAULT true,
                                      is_tax_saver            BOOLEAN NOT NULL DEFAULT false,
                                      lock_in_years           INTEGER NOT NULL DEFAULT 0,
                                      dividend_option         BOOLEAN NOT NULL DEFAULT false,
                                      growth_option           BOOLEAN NOT NULL DEFAULT true,
                                      bse_scheme_code         VARCHAR(20),
                                      nse_symbol              VARCHAR(20),
                                      updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mf_schemes_name ON portfolio.mf_schemes USING gin(to_tsvector('english', scheme_name || ' ' || amc_name));
CREATE INDEX IF NOT EXISTS idx_mf_schemes_category ON portfolio.mf_schemes(category, risk_level) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_mf_schemes_returns ON portfolio.mf_schemes(returns_1y DESC, returns_3y DESC) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_mf_schemes_elss ON portfolio.mf_schemes(is_tax_saver) WHERE is_tax_saver = true AND is_active = true;

-- =====================================================================
-- TABLE 10: mf_investments
-- =====================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'mf_transaction_type' AND n.nspname = 'portfolio') THEN
        CREATE TYPE portfolio.mf_transaction_type AS ENUM ('PURCHASE','REDEMPTION','SIP','SWITCH_IN','SWITCH_OUT','DIVIDEND');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'mf_transaction_status' AND n.nspname = 'portfolio') THEN
        CREATE TYPE portfolio.mf_transaction_status AS ENUM ('PENDING','SUBMITTED','ALLOTTED','REJECTED','CANCELLED');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS portfolio.mf_investments (
                                          id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          user_id             UUID NOT NULL,
                                          scheme_code         VARCHAR(20) NOT NULL REFERENCES portfolio.mf_schemes(scheme_code),
                                          sip_id              UUID,
                                          transaction_type    portfolio.mf_transaction_type NOT NULL,
                                          status              portfolio.mf_transaction_status NOT NULL DEFAULT 'PENDING',
                                          amount              NUMERIC(12,2) NOT NULL CHECK (amount >= 100),
                                          nav_applied         NUMERIC(12,4),
                                          units_allotted      NUMERIC(14,4),
                                          folio_number        VARCHAR(50),
                                          bse_order_id        VARCHAR(50),
                                          bse_remarks         TEXT,
                                          transaction_date    DATE NOT NULL DEFAULT CURRENT_DATE,
                                          allotment_date      DATE,
                                          created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                          updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mf_investments_user_id ON portfolio.mf_investments(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_mf_investments_scheme ON portfolio.mf_investments(scheme_code);

-- =====================================================================
-- TABLE 11: sips
-- =====================================================================
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'sip_status' AND n.nspname = 'portfolio') THEN
        CREATE TYPE portfolio.sip_status AS ENUM ('ACTIVE','PAUSED','CANCELLED','COMPLETED');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace WHERE t.typname = 'sip_frequency' AND n.nspname = 'portfolio') THEN
        CREATE TYPE portfolio.sip_frequency AS ENUM ('MONTHLY','WEEKLY','QUARTERLY');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS portfolio.sips (
                                id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id                     UUID NOT NULL,
                                scheme_code                 VARCHAR(20) NOT NULL REFERENCES portfolio.mf_schemes(scheme_code),
                                goal_id                     UUID,
                                frequency                   portfolio.sip_frequency NOT NULL DEFAULT 'MONTHLY',
                                amount                      NUMERIC(12,2) NOT NULL CHECK (amount >= 100),
                                debit_day                   INTEGER NOT NULL CHECK (debit_day BETWEEN 1 AND 28),
                                status                      portfolio.sip_status NOT NULL DEFAULT 'ACTIVE',
                                upi_mandate_id               VARCHAR(100),
                                upi_mandate_status           VARCHAR(30),
                                razorpay_subscription_id     VARCHAR(100),
                                start_date                  DATE NOT NULL,
                                end_date                    DATE,
                                next_debit_date              DATE,
                                installments_planned         INTEGER,
                                installments_done            INTEGER NOT NULL DEFAULT 0,
                                installments_failed           INTEGER NOT NULL DEFAULT 0,
                                total_invested               NUMERIC(14,2) NOT NULL DEFAULT 0,
                                total_units                  NUMERIC(14,4) NOT NULL DEFAULT 0,
                                paused_at                    TIMESTAMPTZ,
                                paused_reason                TEXT,
                                cancelled_at                  TIMESTAMPTZ,
                                cancelled_reason              TEXT,
                                created_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sips_user_id ON portfolio.sips(user_id, status);
CREATE INDEX IF NOT EXISTS idx_sips_next_debit ON portfolio.sips(next_debit_date, status) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_sips_failed ON portfolio.sips(installments_failed) WHERE status = 'ACTIVE' AND installments_failed > 0;

-- =====================================================================
-- TABLE 16: portfolio_insights
-- =====================================================================
CREATE TABLE IF NOT EXISTS portfolio.portfolio_insights (
                                              id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                              user_id                 UUID NOT NULL,
                                              insight_date            DATE NOT NULL,
                                              language                VARCHAR(5) NOT NULL DEFAULT 'en',
                                              insight_text            TEXT NOT NULL,
                                              portfolio_value         NUMERIC(14,2),
                                              daily_change_pct        NUMERIC(8,4),
                                              top_gainer_symbol       VARCHAR(30),
                                              top_loser_symbol        VARCHAR(30),
                                              market_summary          TEXT,
                                              generation_status       VARCHAR(20) NOT NULL DEFAULT 'GENERATED' CHECK (generation_status IN ('GENERATED','FALLBACK','FAILED')),
                                              llm_model_used          VARCHAR(50),
                                              tokens_used             INTEGER,
                                              generation_time_ms      INTEGER,
                                              created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                              CONSTRAINT uq_portfolio_insight_user_date_lang UNIQUE (user_id, insight_date, language)
);

CREATE INDEX IF NOT EXISTS idx_portfolio_insights_user_date ON portfolio.portfolio_insights(user_id, insight_date DESC, language);
CREATE INDEX IF NOT EXISTS idx_portfolio_insights_fallback ON portfolio.portfolio_insights(insight_date, generation_status)
    WHERE generation_status != 'GENERATED';