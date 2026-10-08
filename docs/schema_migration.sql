-- =============================================================================
-- PaiseWise Complete Database Schema Migration Script
-- 100% Schema-Prefixed, Dependency-Ordered, Single-Run SQL Migration Spec
-- =============================================================================

-- -----------------------------------------------------
-- 1. CREATE SCHEMAS
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS learn;
CREATE SCHEMA IF NOT EXISTS practice;
CREATE SCHEMA IF NOT EXISTS portfolio;
CREATE SCHEMA IF NOT EXISTS community;
CREATE SCHEMA IF NOT EXISTS profile;
CREATE SCHEMA IF NOT EXISTS market;
CREATE SCHEMA IF NOT EXISTS notification;

-- -----------------------------------------------------
-- 2. CREATE ENUM TYPES (IN PUBLIC SCHEMA)
-- -----------------------------------------------------
CREATE TYPE public.kyc_document_status AS ENUM (
  'INITIATED', 'PAN_SUBMITTED', 'DIGILOCKER_COMPLETED', 'VIDEO_KYC_PENDING',
  'VIDEO_KYC_COMPLETED', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED', 'RESUBMISSION_REQUIRED'
);

CREATE TYPE public.kyc_rejection_reason AS ENUM (
  'PAN_MISMATCH', 'NAME_MISMATCH', 'POOR_DOCUMENT_QUALITY', 'FAKE_DOCUMENT_SUSPECTED',
  'INCOMPLETE_SUBMISSION', 'FACE_MISMATCH', 'ADDRESS_MISMATCH', 'MINOR_DETECTED', 'DUPLICATE_PAN',
  'OTHER'
);

CREATE TYPE public.question_type AS ENUM ('MCQ', 'TF', 'TEXT');
CREATE TYPE public.lesson_difficulty AS ENUM ('BEGINNER', 'INTERMEDIATE', 'ADVANCED');
CREATE TYPE public.lesson_progress_status AS ENUM ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED');
CREATE TYPE public.quiz_attempt_status AS ENUM ('IN_PROGRESS', 'PASSED', 'FAILED', 'ABANDONED');
CREATE TYPE public.order_side AS ENUM ('BUY', 'SELL');
CREATE TYPE public.order_type AS ENUM ('MARKET', 'LIMIT', 'SL', 'SL-M');
CREATE TYPE public.product_type AS ENUM ('CNC', 'MIS', 'NRML');
CREATE TYPE public.order_status AS ENUM ('PENDING', 'OPEN', 'PARTIAL', 'COMPLETE', 'REJECTED', 'CANCELLED');
CREATE TYPE public.transaction_type AS ENUM ('CREDIT', 'DEBIT');
CREATE TYPE public.mf_transaction_type AS ENUM ('PURCHASE', 'REDEMPTION', 'SIP', 'SWITCH_IN', 'SWITCH_OUT', 'DIVIDEND');
CREATE TYPE public.mf_transaction_status AS ENUM ('PENDING', 'SUBMITTED', 'ALLOTTED', 'REJECTED', 'CANCELLED');
CREATE TYPE public.sip_status AS ENUM ('ACTIVE', 'PAUSED', 'CANCELLED', 'COMPLETED');
CREATE TYPE public.sip_frequency AS ENUM ('MONTHLY', 'WEEKLY', 'QUARTERLY');
CREATE TYPE public.notification_channel AS ENUM ('PUSH', 'SMS', 'IN_APP', 'EMAIL');
CREATE TYPE public.notification_status AS ENUM ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED');
CREATE TYPE public.holiday_type AS ENUM (
  'NATIONAL_HOLIDAY', 'RELIGIOUS_HOLIDAY', 'SPECIAL_HOLIDAY', 'SETTLEMENT_HOLIDAY',
  'MUHURAT_TRADING'
);
CREATE TYPE public.alert_condition AS ENUM ('GT', 'LT', 'GTE', 'LTE');
CREATE TYPE public.alert_status AS ENUM ('ACTIVE', 'TRIGGERED', 'DELETED', 'EXPIRED');

-- -----------------------------------------------------
-- 3. CREATE INDEPENDENT BASE TABLES
-- -----------------------------------------------------
CREATE TABLE auth.users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  phone VARCHAR(15) NOT NULL UNIQUE,
  email VARCHAR(100) UNIQUE,
  name VARCHAR(150),
  mpin_hash VARCHAR(60),
  kyc_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  xp_points INTEGER NOT NULL DEFAULT 0,
  level INTEGER NOT NULL DEFAULT 1,
  streak_days INTEGER NOT NULL DEFAULT 0,
  last_active_date DATE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE learn.lessons (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  chapter_key VARCHAR(50) NOT NULL,
  chapter_name_en VARCHAR(100) NOT NULL,
  chapter_name_hi VARCHAR(100) NOT NULL,
  chapter_order INTEGER NOT NULL DEFAULT 0,
  lesson_order INTEGER NOT NULL DEFAULT 0,
  difficulty public.lesson_difficulty NOT NULL DEFAULT 'BEGINNER',
  title_en VARCHAR(200) NOT NULL,
  title_hi VARCHAR(200) NOT NULL,
  subtitle_en VARCHAR(300),
  subtitle_hi VARCHAR(300),
  content_en JSONB NOT NULL DEFAULT '[]',
  content_hi JSONB NOT NULL DEFAULT '[]',
  xp_reward INTEGER NOT NULL DEFAULT 50 CHECK (xp_reward > 0),
  estimated_minutes INTEGER NOT NULL DEFAULT 3 CHECK (estimated_minutes > 0),
  total_blocks INTEGER NOT NULL DEFAULT 0,
  prerequisite_lesson_id UUID REFERENCES learn.lessons(id),
  min_level_required INTEGER NOT NULL DEFAULT 1,
  thumbnail_url TEXT,
  tags TEXT[] NOT NULL DEFAULT '{}',
  is_published BOOLEAN NOT NULL DEFAULT false,
  is_premium BOOLEAN NOT NULL DEFAULT false,
  published_at TIMESTAMPTZ,
  completion_count INTEGER NOT NULL DEFAULT 0,
  avg_time_seconds INTEGER,
  avg_quiz_score NUMERIC(5,2),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_lesson_order UNIQUE (chapter_key, lesson_order)
);

CREATE TABLE learn.jargon_terms (
  term VARCHAR(100) PRIMARY KEY,
  term_display VARCHAR(100) NOT NULL,
  category VARCHAR(50),
  definition_en TEXT NOT NULL,
  definition_hi TEXT,
  definition_ta TEXT,
  definition_te TEXT,
  definition_mr TEXT,
  definition_bn TEXT,
  definition_gu TEXT,
  definition_kn TEXT,
  analogy_en TEXT,
  analogy_hi TEXT,
  example_en TEXT,
  example_hi TEXT,
  related_terms TEXT[] NOT NULL DEFAULT '{}',
  difficulty public.lesson_difficulty NOT NULL DEFAULT 'BEGINNER',
  source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'AI_GENERATED', 'AI_REVIEWED')),
  ai_model_used VARCHAR(50),
  tap_count INTEGER NOT NULL DEFAULT 0,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE profile.badge_definitions (
  badge_key VARCHAR(50) PRIMARY KEY,
  category VARCHAR(50) NOT NULL,
  name_en VARCHAR(100) NOT NULL,
  name_hi VARCHAR(100) NOT NULL,
  description_en TEXT NOT NULL,
  description_hi TEXT NOT NULL,
  icon_emoji VARCHAR(10) NOT NULL,
  xp_bonus INTEGER NOT NULL DEFAULT 0,
  unlock_criteria_en TEXT NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE market.symbols (
  symbol VARCHAR(30) PRIMARY KEY,
  company_name VARCHAR(200) NOT NULL,
  short_name VARCHAR(50),
  exchange VARCHAR(5) NOT NULL CHECK (exchange IN ('NSE', 'BSE', 'NFO', 'BFO', 'MCX')),
  instrument_type VARCHAR(20) NOT NULL CHECK (instrument_type IN ('EQ', 'BE', 'INDEX', 'FUT', 'CE', 'PE', 'ETF', 'REIT', 'INVIT')),
  isin VARCHAR(12),
  series VARCHAR(5),
  sector VARCHAR(100),
  industry VARCHAR(100),
  market_cap_category VARCHAR(10) CHECK (market_cap_category IN ('LARGE', 'MID', 'SMALL', 'MICRO', 'UNKNOWN')),
  face_value NUMERIC(10,2),
  lot_size INTEGER NOT NULL DEFAULT 1,
  tick_size NUMERIC(10,4) NOT NULL DEFAULT 0.05,
  is_fo_enabled BOOLEAN NOT NULL DEFAULT false,
  is_slb_enabled BOOLEAN NOT NULL DEFAULT false,
  is_active BOOLEAN NOT NULL DEFAULT true,
  is_suspended BOOLEAN NOT NULL DEFAULT false,
  suspension_reason TEXT,
  upper_circuit_pct NUMERIC(5,2),
  lower_circuit_pct NUMERIC(5,2),
  bse_code VARCHAR(10),
  nse_token INTEGER,
  fyers_symbol VARCHAR(30),
  logo_url TEXT,
  description TEXT,
  website_url TEXT,
  listing_date DATE,
  demat_lot_size INTEGER,
  last_synced_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE market.exchange_holidays (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  holiday_date DATE NOT NULL,
  exchange VARCHAR(5) NOT NULL CHECK (exchange IN ('NSE', 'BSE', 'MCX', 'ALL')),
  holiday_name VARCHAR(100) NOT NULL,
  holiday_type public.holiday_type NOT NULL,
  is_trading_day BOOLEAN NOT NULL DEFAULT false,
  trading_start TIME,
  trading_end TIME,
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_exchange_holiday UNIQUE (holiday_date, exchange)
);

CREATE TABLE portfolio.mf_schemes (
  scheme_code VARCHAR(20) PRIMARY KEY,
  isin VARCHAR(12) UNIQUE,
  scheme_name VARCHAR(300) NOT NULL,
  amc_name VARCHAR(100) NOT NULL,
  amc_code VARCHAR(20),
  category VARCHAR(50) NOT NULL,
  sub_category VARCHAR(50),
  scheme_type VARCHAR(20) NOT NULL CHECK (scheme_type IN ('Open Ended', 'Close Ended', 'Interval')),
  risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN ('Low', 'Low to Moderate', 'Moderate', 'Moderately High', 'High', 'Very High')),
  nav NUMERIC(12,4),
  nav_date DATE,
  min_sip_amount NUMERIC(10,2) NOT NULL DEFAULT 100,
  min_lumpsum NUMERIC(10,2) NOT NULL DEFAULT 1000,
  sip_multiplier NUMERIC(10,2) NOT NULL DEFAULT 1,
  returns_1y NUMERIC(8,4),
  returns_3y NUMERIC(8,4),
  returns_5y NUMERIC(8,4),
  returns_since_launch NUMERIC(8,4),
  benchmark_name VARCHAR(100),
  benchmark_returns_1y NUMERIC(8,4),
  expense_ratio NUMERIC(5,4),
  fund_manager VARCHAR(200),
  fund_size_cr NUMERIC(14,2),
  launch_date DATE,
  is_active BOOLEAN NOT NULL DEFAULT true,
  is_tax_saver BOOLEAN NOT NULL DEFAULT false,
  lock_in_years INTEGER NOT NULL DEFAULT 0,
  dividend_option BOOLEAN NOT NULL DEFAULT false,
  growth_option BOOLEAN NOT NULL DEFAULT true,
  bse_scheme_code VARCHAR(20),
  nse_symbol VARCHAR(20),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------
-- 4. CREATE TABLES WITH FOREIGN KEY DEPENDENCIES
-- -----------------------------------------------------
CREATE TABLE auth.refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE auth.otp_verifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  phone VARCHAR(15) NOT NULL,
  otp_hash VARCHAR(60) NOT NULL,
  purpose VARCHAR(30) NOT NULL DEFAULT 'LOGIN' CHECK (purpose IN ('LOGIN', 'REGISTRATION', 'MPIN_RESET', 'KYC_VERIFY', 'WITHDRAWAL')),
  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  max_attempts INTEGER NOT NULL DEFAULT 3,
  is_used BOOLEAN NOT NULL DEFAULT false,
  is_locked BOOLEAN NOT NULL DEFAULT false,
  locked_until TIMESTAMPTZ,
  ip_address VARCHAR(45),
  device_id VARCHAR(200),
  expires_at TIMESTAMPTZ NOT NULL,
  verified_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE auth.kyc_documents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id),
  status public.kyc_document_status NOT NULL DEFAULT 'INITIATED',
  attempt_number INTEGER NOT NULL DEFAULT 1,
  pan_encrypted TEXT,
  pan_last4 VARCHAR(4),
  pan_name VARCHAR(200),
  pan_dob DATE,
  pan_verified_at TIMESTAMPTZ,
  aadhaar_last4 VARCHAR(4),
  aadhaar_name VARCHAR(200),
  aadhaar_dob DATE,
  aadhaar_gender VARCHAR(10),
  aadhaar_address JSONB,
  aadhaar_verified_at TIMESTAMPTZ,
  digilocker_state VARCHAR(100),
  digilocker_code TEXT,
  digilocker_access_token TEXT,
  digilocker_ref_id VARCHAR(100),
  digilocker_completed_at TIMESTAMPTZ,
  pan_document_s3_key TEXT,
  pan_document_s3_bucket VARCHAR(100),
  selfie_s3_key TEXT,
  selfie_s3_bucket VARCHAR(100),
  aadhaar_xml_s3_key TEXT,
  video_kyc_provider VARCHAR(50),
  video_kyc_ref_id VARCHAR(100),
  video_kyc_url TEXT,
  video_kyc_status VARCHAR(30),
  video_kyc_score NUMERIC(5,4),
  video_kyc_completed_at TIMESTAMPTZ,
  name_match_score NUMERIC(5,4),
  name_match_passed BOOLEAN,
  reviewed_by UUID REFERENCES auth.users(id),
  reviewed_at TIMESTAMPTZ,
  rejection_reason public.kyc_rejection_reason,
  rejection_note TEXT,
  reviewer_internal_note TEXT,
  submitted_at TIMESTAMPTZ,
  verified_at TIMESTAMPTZ,
  rejected_at TIMESTAMPTZ,
  ip_address VARCHAR(45),
  device_id VARCHAR(200),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_kyc_user_attempt UNIQUE (user_id, attempt_number)
);

CREATE TABLE learn.quiz_questions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  lesson_id UUID NOT NULL REFERENCES learn.lessons(id) ON DELETE CASCADE,
  question_type public.question_type NOT NULL DEFAULT 'MCQ',
  order_index INTEGER NOT NULL DEFAULT 0,
  difficulty public.lesson_difficulty NOT NULL DEFAULT 'BEGINNER',
  question_en TEXT NOT NULL,
  question_hi TEXT NOT NULL,
  options_en JSONB NOT NULL DEFAULT '[]',
  options_hi JSONB NOT NULL DEFAULT '[]',
  correct_option_id VARCHAR(5) NOT NULL,
  explanation_en TEXT NOT NULL,
  explanation_hi TEXT NOT NULL,
  correct_boolean BOOLEAN,
  hint_en TEXT,
  hint_hi TEXT,
  times_shown INTEGER NOT NULL DEFAULT 0,
  times_correct INTEGER NOT NULL DEFAULT 0,
  correct_rate NUMERIC(5,4) NOT NULL DEFAULT 0,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE learn.quiz_attempts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id),
  lesson_id UUID NOT NULL REFERENCES learn.lessons(id),
  attempt_number INTEGER NOT NULL DEFAULT 1,
  status public.quiz_attempt_status NOT NULL DEFAULT 'IN_PROGRESS',
  questions_served JSONB NOT NULL DEFAULT '[]',
  user_answers JSONB NOT NULL DEFAULT '[]',
  total_questions INTEGER NOT NULL DEFAULT 0,
  correct_answers INTEGER NOT NULL DEFAULT 0,
  score_pct NUMERIC(6,3) NOT NULL DEFAULT 0.000 CHECK (score_pct BETWEEN 0 AND 100),
  pass_threshold_pct NUMERIC(6,3) NOT NULL DEFAULT 60.000,
  passed BOOLEAN NOT NULL DEFAULT false,
  xp_earned INTEGER NOT NULL DEFAULT 0,
  xp_bonus INTEGER NOT NULL DEFAULT 0,
  time_spent_seconds INTEGER NOT NULL DEFAULT 0,
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  submitted_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  language VARCHAR(5) NOT NULL DEFAULT 'en'
);

CREATE TABLE learn.user_lesson_progress (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id),
  lesson_id UUID NOT NULL REFERENCES learn.lessons(id),
  status public.lesson_progress_status NOT NULL DEFAULT 'NOT_STARTED',
  current_block_index INTEGER NOT NULL DEFAULT 0,
  total_blocks INTEGER NOT NULL DEFAULT 0,
  scroll_position_pct NUMERIC(5,2) NOT NULL DEFAULT 0.00 CHECK (scroll_position_pct BETWEEN 0 AND 100),
  time_spent_seconds INTEGER NOT NULL DEFAULT 0,
  jargon_taps INTEGER NOT NULL DEFAULT 0,
  language VARCHAR(5) NOT NULL DEFAULT 'en',
  xp_earned INTEGER NOT NULL DEFAULT 0,
  completed_at TIMESTAMPTZ,
  last_viewed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_user_lesson_progress UNIQUE (user_id, lesson_id)
);

CREATE TABLE profile.user_badges (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  badge_key VARCHAR(50) NOT NULL REFERENCES profile.badge_definitions(badge_key) ON DELETE CASCADE,
  earned_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE profile.user_features (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id),
  lessons_completed_total INTEGER NOT NULL DEFAULT 0,
  lessons_completed_7d INTEGER NOT NULL DEFAULT 0,
  lessons_completed_30d INTEGER NOT NULL DEFAULT 0,
  quiz_attempts_total INTEGER NOT NULL DEFAULT 0,
  quiz_pass_rate NUMERIC(5,4) NOT NULL DEFAULT 0,
  avg_quiz_score NUMERIC(5,4) NOT NULL DEFAULT 0,
  chapters_completed INTEGER NOT NULL DEFAULT 0,
  jargon_taps_7d INTEGER NOT NULL DEFAULT 0,
  streak_days_current INTEGER NOT NULL DEFAULT 0,
  streak_days_longest INTEGER NOT NULL DEFAULT 0,
  sessions_7d INTEGER NOT NULL DEFAULT 0,
  sessions_30d INTEGER NOT NULL DEFAULT 0,
  avg_session_duration_secs INTEGER NOT NULL DEFAULT 0,
  days_since_last_active INTEGER NOT NULL DEFAULT 0,
  days_since_registration INTEGER NOT NULL DEFAULT 0,
  notification_open_rate_30d NUMERIC(5,4) NOT NULL DEFAULT 0,
  paper_trades_total INTEGER NOT NULL DEFAULT 0,
  paper_trades_7d INTEGER NOT NULL DEFAULT 0,
  has_real_investment BOOLEAN NOT NULL DEFAULT false,
  churn_score NUMERIC(5,4) NOT NULL DEFAULT 0.0000 CHECK (churn_score BETWEEN 0 AND 1),
  churn_score_computed_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE profile.portfolio_insights (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  insight_date DATE NOT NULL,
  language VARCHAR(5) NOT NULL DEFAULT 'en',
  insight_text TEXT NOT NULL,
  portfolio_value NUMERIC(14,2),
  daily_change_pct NUMERIC(8,4),
  top_gainer_symbol VARCHAR(30),
  top_loser_symbol VARCHAR(30),
  market_summary TEXT,
  generation_status VARCHAR(20) NOT NULL DEFAULT 'GENERATED' CHECK (generation_status IN ('GENERATED', 'FALLBACK', 'FAILED')),
  llm_model_used VARCHAR(50),
  tokens_used INTEGER,
  generation_time_ms INTEGER,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_portfolio_insight_user_date_lang UNIQUE (user_id, insight_date, language)
);

CREATE TABLE market.ticks (
  time TIMESTAMPTZ NOT NULL,
  symbol VARCHAR(30) NOT NULL REFERENCES market.symbols(symbol) ON DELETE CASCADE,
  ltp NUMERIC(12,2) NOT NULL,
  open NUMERIC(12,2),
  high NUMERIC(12,2),
  low NUMERIC(12,2),
  close NUMERIC(12,2),
  prev_close NUMERIC(12,2),
  volume BIGINT,
  avg_price NUMERIC(12,2),
  upper_circuit NUMERIC(12,2),
  lower_circuit NUMERIC(12,2),
  oi BIGINT,
  oi_day_high BIGINT,
  oi_day_low BIGINT,
  bid_price NUMERIC(12,2),
  ask_price NUMERIC(12,2),
  bid_qty INTEGER,
  ask_qty INTEGER,
  change_abs NUMERIC(12,2),
  change_pct NUMERIC(8,4),
  PRIMARY KEY (time, symbol)
);

CREATE TABLE market.watchlists (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  symbol VARCHAR(30) NOT NULL REFERENCES market.symbols(symbol) ON DELETE CASCADE,
  sort_order INTEGER NOT NULL DEFAULT 0,
  added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_watchlist_user_symbol UNIQUE (user_id, symbol)
);

CREATE TABLE practice.price_alerts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  symbol VARCHAR(30) NOT NULL REFERENCES market.symbols(symbol) ON DELETE CASCADE,
  condition public.alert_condition NOT NULL,
  target_price NUMERIC(12,2) NOT NULL CHECK (target_price > 0),
  ltp_at_creation NUMERIC(12,2) NOT NULL,
  status public.alert_status NOT NULL DEFAULT 'ACTIVE',
  triggered_at TIMESTAMPTZ,
  triggered_price NUMERIC(12,2),
  notification_sent BOOLEAN NOT NULL DEFAULT false,
  note VARCHAR(200),
  expires_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT chk_target_price_makes_sense CHECK (
    (condition = 'GT' AND target_price > ltp_at_creation) OR
    (condition = 'GTE' AND target_price >= ltp_at_creation) OR
    (condition = 'LT' AND target_price < ltp_at_creation) OR
    (condition = 'LTE' AND target_price <= ltp_at_creation)
  )
);

CREATE TABLE practice.orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id),
  client_order_id VARCHAR(64) NOT NULL UNIQUE,
  symbol VARCHAR(30) NOT NULL REFERENCES market.symbols(symbol),
  exchange VARCHAR(5) NOT NULL CHECK (exchange IN ('NSE', 'BSE')),
  side public.order_side NOT NULL,
  order_type public.order_type NOT NULL,
  product public.product_type NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  filled_qty INTEGER NOT NULL DEFAULT 0,
  price NUMERIC(12,2),
  trigger_price NUMERIC(12,2),
  avg_price NUMERIC(12,4),
  status public.order_status NOT NULL DEFAULT 'PENDING',
  broker_order_id VARCHAR(50),
  broker_message TEXT,
  is_paper BOOLEAN NOT NULL DEFAULT false,
  validity VARCHAR(5) NOT NULL DEFAULT 'DAY' CHECK (validity IN ('DAY','IOC')),
  placed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE practice.trades (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES practice.orders(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id),
  symbol VARCHAR(30) NOT NULL,
  exchange VARCHAR(5) NOT NULL,
  side public.order_side NOT NULL,
  fill_qty INTEGER NOT NULL CHECK (fill_qty > 0),
  fill_price NUMERIC(12,4) NOT NULL,
  brokerage NUMERIC(10,2) NOT NULL DEFAULT 0,
  stt NUMERIC(10,4) NOT NULL DEFAULT 0,
  gst NUMERIC(10,4) NOT NULL DEFAULT 0,
  sebi_charges NUMERIC(10,6) NOT NULL DEFAULT 0,
  stamp_duty NUMERIC(10,4) NOT NULL DEFAULT 0,
  total_charges NUMERIC(10,4) NOT NULL DEFAULT 0,
  net_amount NUMERIC(14,4) NOT NULL,
  broker_trade_id VARCHAR(50) UNIQUE,
  is_paper BOOLEAN NOT NULL DEFAULT false,
  traded_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE practice.ledger (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES auth.users(id),
  type public.transaction_type NOT NULL,
  amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
  balance_after NUMERIC(14,2) NOT NULL,
  description VARCHAR(200) NOT NULL,
  ref_type VARCHAR(30),
  ref_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE practice.holdings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  symbol VARCHAR(30) NOT NULL REFERENCES market.symbols(symbol),
  quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
  avg_cost NUMERIC(12,4) NOT NULL,
  total_invested NUMERIC(14,2) NOT NULL,
  product public.product_type NOT NULL DEFAULT 'CNC',
  is_paper BOOLEAN NOT NULL DEFAULT false,
  first_bought_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_holdings_user_symbol UNIQUE (user_id, symbol, product, is_paper)
);

CREATE TABLE portfolio.sips (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id),
  scheme_code VARCHAR(20) NOT NULL REFERENCES portfolio.mf_schemes(scheme_code),
  goal_id UUID,
  frequency public.sip_frequency NOT NULL DEFAULT 'MONTHLY',
  amount NUMERIC(12,2) NOT NULL CHECK (amount >= 100),
  debit_day INTEGER NOT NULL CHECK (debit_day BETWEEN 1 AND 28),
  status public.sip_status NOT NULL DEFAULT 'ACTIVE',
  upi_mandate_id VARCHAR(100),
  upi_mandate_status VARCHAR(30),
  razorpay_subscription_id VARCHAR(100),
  start_date DATE NOT NULL,
  end_date DATE,
  next_debit_date DATE,
  installments_planned INTEGER,
  installments_done INTEGER NOT NULL DEFAULT 0,
  installments_failed INTEGER NOT NULL DEFAULT 0,
  total_invested NUMERIC(14,2) NOT NULL DEFAULT 0,
  total_units NUMERIC(14,4) NOT NULL DEFAULT 0,
  paused_at TIMESTAMPTZ,
  paused_reason TEXT,
  cancelled_at TIMESTAMPTZ,
  cancelled_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE portfolio.mf_investments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id),
  scheme_code VARCHAR(20) NOT NULL REFERENCES portfolio.mf_schemes(scheme_code),
  sip_id UUID REFERENCES portfolio.sips(id),
  transaction_type public.mf_transaction_type NOT NULL,
  status public.mf_transaction_status NOT NULL DEFAULT 'PENDING',
  amount NUMERIC(12,2) NOT NULL CHECK (amount >= 100),
  nav_applied NUMERIC(12,4),
  units_allotted NUMERIC(14,4),
  folio_number VARCHAR(50),
  bse_order_id VARCHAR(50),
  bse_remarks TEXT,
  transaction_date DATE NOT NULL DEFAULT CURRENT_DATE,
  allotment_date DATE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notification.notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id),
  channel public.notification_channel NOT NULL,
  type VARCHAR(50) NOT NULL,
  title VARCHAR(200) NOT NULL,
  body TEXT NOT NULL,
  data JSONB NOT NULL DEFAULT '{}',
  status public.notification_status NOT NULL DEFAULT 'PENDING',
  fcm_message_id VARCHAR(200),
  error_message TEXT,
  is_read BOOLEAN NOT NULL DEFAULT false,
  read_at TIMESTAMPTZ,
  deep_link VARCHAR(200),
  image_url VARCHAR(500),
  sent_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE community.community_posts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  body TEXT NOT NULL CHECK (char_length(body) BETWEEN 10 AND 1000),
  language VARCHAR(5) NOT NULL DEFAULT 'hi' CHECK (language IN ('hi','en','ta','te','mr','bn','gu','kn')),
  tags TEXT[] NOT NULL DEFAULT '{}',
  upvote_count INTEGER NOT NULL DEFAULT 0 CHECK (upvote_count >= 0),
  answer_count INTEGER NOT NULL DEFAULT 0 CHECK (answer_count >= 0),
  view_count INTEGER NOT NULL DEFAULT 0,
  is_answered BOOLEAN NOT NULL DEFAULT false,
  accepted_answer_id UUID,
  is_removed BOOLEAN NOT NULL DEFAULT false,
  removed_reason VARCHAR(100),
  removed_by UUID REFERENCES auth.users(id),
  is_pinned BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE community.community_answers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id UUID NOT NULL REFERENCES community.community_posts(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  body TEXT NOT NULL CHECK (char_length(body) BETWEEN 10 AND 2000),
  language VARCHAR(5) NOT NULL DEFAULT 'hi',
  upvote_count INTEGER NOT NULL DEFAULT 0 CHECK (upvote_count >= 0),
  is_verified_helper BOOLEAN NOT NULL DEFAULT false,
  is_accepted BOOLEAN NOT NULL DEFAULT false,
  is_removed BOOLEAN NOT NULL DEFAULT false,
  removed_reason VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- CREATE PARTITIONED AUDIT LOG TABLE
CREATE TABLE public.audit_log (
  id BIGSERIAL,
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  action VARCHAR(60) NOT NULL,
  entity_type VARCHAR(30) NOT NULL,
  entity_id UUID,
  old_values JSONB,
  new_values JSONB,
  ip_address VARCHAR(45),
  user_agent TEXT,
  device_id VARCHAR(200),
  session_id VARCHAR(100),
  request_id VARCHAR(100),
  result VARCHAR(10) NOT NULL DEFAULT 'SUCCESS' CHECK (result IN ('SUCCESS', 'FAILURE')),
  failure_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Partition Tables for audit_log
CREATE TABLE public.audit_log_2025 PARTITION OF public.audit_log
  FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE TABLE public.audit_log_2026 PARTITION OF public.audit_log
  FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

-- -----------------------------------------------------
-- 5. SETUP TIMESCALEDB HYPERTABLES & AGGREGATES
-- -----------------------------------------------------
-- Convert ticks table into a TimescaleDB hypertable
SELECT create_hypertable('market.ticks', 'time', chunk_time_interval => INTERVAL '1 day', if_not_exists => TRUE);

-- 1-minute candles continuous aggregate
CREATE MATERIALIZED VIEW market.candles_1min
WITH (timescaledb.continuous) AS
SELECT time_bucket('1 minute', time) AS bucket,
  symbol,
  FIRST(ltp, time) AS open,
  MAX(ltp) AS high,
  MIN(ltp) AS low,
  LAST(ltp, time) AS close,
  MAX(volume) - MIN(volume) AS volume,
  COUNT(*) AS tick_count
FROM market.ticks
GROUP BY bucket, symbol WITH NO DATA;

SELECT add_continuous_aggregate_policy('market.candles_1min',
  start_offset => INTERVAL '2 hours',
  end_offset => INTERVAL '1 minute',
  schedule_interval => INTERVAL '1 minute');

-- 5-minute candles continuous aggregate
CREATE MATERIALIZED VIEW market.candles_5min
WITH (timescaledb.continuous) AS
SELECT time_bucket('5 minutes', time) AS bucket,
  symbol,
  FIRST(ltp, time) AS open,
  MAX(ltp) AS high,
  MIN(ltp) AS low,
  LAST(ltp, time) AS close,
  MAX(volume) - MIN(volume) AS volume
FROM market.ticks
GROUP BY bucket, symbol WITH NO DATA;

SELECT add_continuous_aggregate_policy('market.candles_5min',
  start_offset => INTERVAL '4 hours',
  end_offset => INTERVAL '5 minutes',
  schedule_interval => INTERVAL '5 minutes');

-- 15-minute candles continuous aggregate
CREATE MATERIALIZED VIEW market.candles_15min
WITH (timescaledb.continuous) AS
SELECT time_bucket('15 minutes', time) AS bucket,
  symbol,
  FIRST(ltp, time) AS open,
  MAX(ltp) AS high,
  MIN(ltp) AS low,
  LAST(ltp, time) AS close,
  MAX(volume) - MIN(volume) AS volume
FROM market.ticks
GROUP BY bucket, symbol WITH NO DATA;

SELECT add_continuous_aggregate_policy('market.candles_15min',
  start_offset => INTERVAL '8 hours',
  end_offset => INTERVAL '15 minutes',
  schedule_interval => INTERVAL '15 minutes');

-- 1-hour candles continuous aggregate
CREATE MATERIALIZED VIEW market.candles_1hr
WITH (timescaledb.continuous) AS
SELECT time_bucket('1 hour', time) AS bucket,
  symbol,
  FIRST(ltp, time) AS open,
  MAX(ltp) AS high,
  MIN(ltp) AS low,
  LAST(ltp, time) AS close,
  MAX(volume) - MIN(volume) AS volume
FROM market.ticks
GROUP BY bucket, symbol WITH NO DATA;

SELECT add_continuous_aggregate_policy('market.candles_1hr',
  start_offset => INTERVAL '2 days',
  end_offset => INTERVAL '1 hour',
  schedule_interval => INTERVAL '1 hour');

-- Daily candles continuous aggregate
CREATE MATERIALIZED VIEW market.candles_1d
WITH (timescaledb.continuous) AS
SELECT time_bucket('1 day', time) AS bucket,
  symbol,
  FIRST(ltp, time) AS open,
  MAX(ltp) AS high,
  MIN(ltp) AS low,
  LAST(ltp, time) AS close,
  MAX(volume) AS volume
FROM market.ticks
GROUP BY bucket, symbol WITH NO DATA;

SELECT add_continuous_aggregate_policy('market.candles_1d',
  start_offset => INTERVAL '30 days',
  end_offset => INTERVAL '1 day',
  schedule_interval => INTERVAL '1 day');

-- Set retention & compression policies
SELECT add_retention_policy('market.ticks', INTERVAL '1 year');

ALTER TABLE market.ticks SET (
  timescaledb.compress,
  timescaledb.compress_segmentby = 'symbol',
  timescaledb.compress_orderby = 'time DESC'
);

SELECT add_compression_policy('market.ticks', INTERVAL '7 days');

-- -----------------------------------------------------
-- 6. CREATE TRIGGER FUNCTIONS (SCHEMA-PREFIXED)
-- -----------------------------------------------------
CREATE OR REPLACE FUNCTION auth.update_kyc_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION auth.sync_kyc_status_to_user()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.status = 'VERIFIED' AND OLD.status != 'VERIFIED' THEN
    UPDATE auth.users
    SET kyc_status = 'VERIFIED',
        updated_at = NOW()
    WHERE id = NEW.user_id;
  ELSIF NEW.status = 'REJECTED' AND OLD.status != 'REJECTED' THEN
    UPDATE auth.users
    SET kyc_status = 'REJECTED',
        updated_at = NOW()
    WHERE id = NEW.user_id;
  ELSIF NEW.status = 'UNDER_REVIEW' AND OLD.status != 'UNDER_REVIEW' THEN
    UPDATE auth.users
    SET kyc_status = 'IN_REVIEW',
        updated_at = NOW()
    WHERE id = NEW.user_id;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION auth.prevent_full_aadhaar()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.aadhaar_last4 IS NOT NULL AND length(NEW.aadhaar_last4) != 4 THEN
    RAISE EXCEPTION 'SECURITY VIOLATION: Aadhaar must be last 4 digits only.';
  END IF;
  IF NEW.aadhaar_address::TEXT ~ '\d{12}' THEN
    RAISE EXCEPTION 'SECURITY VIOLATION: Possible full Aadhaar number detected in aadhaar_address field.';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION learn.update_lesson_total_blocks()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.content_en IS DISTINCT FROM OLD.content_en THEN
    NEW.total_blocks := jsonb_array_length(NEW.content_en);
  END IF;
  NEW.updated_at := NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION learn.update_lesson_stats()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE learn.lessons
  SET completion_count = completion_count + 1,
      avg_time_seconds = (
        SELECT AVG(time_spent_seconds)
        FROM learn.user_lesson_progress
        WHERE lesson_id = NEW.lesson_id
          AND status = 'COMPLETED'
      ),
      updated_at = NOW()
  WHERE id = NEW.lesson_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION learn.update_question_stats()
RETURNS TRIGGER AS $$
DECLARE
  answer_record JSONB;
  question_id UUID;
  was_correct BOOLEAN;
BEGIN
  IF NEW.status IN ('PASSED', 'FAILED') AND OLD.status = 'IN_PROGRESS' THEN
    FOR answer_record IN SELECT jsonb_array_elements(NEW.user_answers)
    LOOP
      question_id := (answer_record->>'question_id')::UUID;
      was_correct := (answer_record->>'is_correct')::BOOLEAN;
      UPDATE learn.quiz_questions
      SET times_shown = times_shown + 1,
          times_correct = times_correct + CASE WHEN was_correct THEN 1 ELSE 0 END,
          correct_rate = (times_correct + CASE WHEN was_correct THEN 1 ELSE 0 END)::NUMERIC / (times_shown + 1),
          updated_at = NOW()
      WHERE id = question_id;
    END LOOP;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION learn.validate_correct_option()
RETURNS TRIGGER AS $$
DECLARE
  option_exists BOOLEAN;
BEGIN
  IF NEW.question_type = 'MCQ' THEN
    SELECT EXISTS(
      SELECT 1
      FROM jsonb_array_elements(NEW.options_en) AS opt
      WHERE opt->>'id' = NEW.correct_option_id
    ) INTO option_exists;
    IF NOT option_exists THEN
      RAISE EXCEPTION 'INVALID_ANSWER: correct_option_id not found in options_en';
    END IF;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION learn.increment_jargon_tap_count()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION learn.award_lesson_completion_xp()
RETURNS TRIGGER AS $$
DECLARE
  lesson_xp_reward INTEGER;
BEGIN
  IF NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED' THEN
    SELECT xp_reward INTO lesson_xp_reward
    FROM learn.lessons
    WHERE id = NEW.lesson_id;
    
    IF NEW.xp_earned = 0 AND lesson_xp_reward > 0 THEN
      UPDATE auth.users
      SET xp_points = xp_points + lesson_xp_reward,
          level = FLOOR((xp_points + lesson_xp_reward) / 500.0) + 1,
          updated_at = NOW()
      WHERE id = NEW.user_id;
      NEW.xp_earned := lesson_xp_reward;
      NEW.completed_at := NOW();
    END IF;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION learn.update_streak_on_lesson_complete()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED' THEN
    UPDATE auth.users
    SET last_active_date = CURRENT_DATE,
        streak_days = CASE
          WHEN last_active_date = CURRENT_DATE - INTERVAL '1 day' THEN streak_days + 1
          WHEN last_active_date = CURRENT_DATE THEN streak_days
          ELSE 1
        END,
        updated_at = NOW()
    WHERE id = NEW.user_id;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION learn.award_quiz_xp()
RETURNS TRIGGER AS $$
DECLARE
  base_xp INTEGER := 50;
  bonus_xp INTEGER := 0;
BEGIN
  IF NEW.status = 'PASSED' AND OLD.status != 'PASSED' THEN
    IF NEW.score_pct = 100.000 THEN
      bonus_xp := 50;
    END IF;
    IF NEW.attempt_number = 1 THEN
      bonus_xp := bonus_xp + 10;
    END IF;
    
    IF NEW.xp_earned = 0 THEN
      UPDATE auth.users
      SET xp_points = xp_points + base_xp + bonus_xp,
          level = FLOOR((xp_points + base_xp + bonus_xp) / 500.0) + 1,
          updated_at = NOW()
      WHERE id = NEW.user_id;
      NEW.xp_earned := base_xp;
      NEW.xp_bonus := bonus_xp;
      NEW.completed_at := NOW();
    END IF;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- -----------------------------------------------------
-- 7. BIND TRIGGERS & INDEX CREATIONS
-- -----------------------------------------------------
-- Auth triggers
CREATE TRIGGER trg_kyc_documents_updated_at
  BEFORE UPDATE ON auth.kyc_documents
  FOR EACH ROW EXECUTE FUNCTION auth.update_kyc_updated_at();

CREATE TRIGGER trg_sync_kyc_to_user
  AFTER UPDATE OF status ON auth.kyc_documents
  FOR EACH ROW EXECUTE FUNCTION auth.sync_kyc_status_to_user();

CREATE TRIGGER trg_prevent_full_aadhaar
  BEFORE INSERT OR UPDATE ON auth.kyc_documents
  FOR EACH ROW EXECUTE FUNCTION auth.prevent_full_aadhaar();

-- Learn triggers
CREATE TRIGGER trg_lesson_total_blocks
  BEFORE UPDATE ON learn.lessons
  FOR EACH ROW EXECUTE FUNCTION learn.update_lesson_total_blocks();

CREATE TRIGGER trg_update_lesson_stats
  AFTER UPDATE OF status ON learn.user_lesson_progress
  FOR EACH ROW WHEN (NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED')
  EXECUTE FUNCTION learn.update_lesson_stats();

CREATE TRIGGER trg_update_question_stats
  AFTER UPDATE OF status ON learn.quiz_attempts
  FOR EACH ROW EXECUTE FUNCTION learn.update_question_stats();

CREATE TRIGGER trg_validate_correct_option
  BEFORE INSERT OR UPDATE ON learn.quiz_questions
  FOR EACH ROW EXECUTE FUNCTION learn.validate_correct_option();

CREATE TRIGGER trg_jargon_updated_at
  BEFORE UPDATE ON learn.jargon_terms
  FOR EACH ROW EXECUTE FUNCTION learn.increment_jargon_tap_count();

CREATE TRIGGER trg_award_lesson_xp
  BEFORE UPDATE OF status ON learn.user_lesson_progress
  FOR EACH ROW EXECUTE FUNCTION learn.award_lesson_completion_xp();

CREATE TRIGGER trg_update_streak_on_lesson
  AFTER UPDATE OF status ON learn.user_lesson_progress
  FOR EACH ROW EXECUTE FUNCTION learn.update_streak_on_lesson_complete();

CREATE TRIGGER trg_award_quiz_xp
  BEFORE UPDATE OF status ON learn.quiz_attempts
  FOR EACH ROW EXECUTE FUNCTION learn.award_quiz_xp();

-- Create optimized indices
CREATE INDEX idx_otp_verifications_phone_purpose ON auth.otp_verifications(phone, purpose, created_at DESC) WHERE is_used = false AND is_locked = false;
CREATE INDEX idx_kyc_documents_user_latest ON auth.kyc_documents(user_id, attempt_number DESC);
CREATE INDEX idx_lessons_chapter_order ON learn.lessons(chapter_key, lesson_order ASC) WHERE is_published = true;
CREATE INDEX idx_quiz_questions_lesson ON learn.quiz_questions(lesson_id, order_index ASC) WHERE is_active = true;
CREATE INDEX idx_jargon_category ON learn.jargon_terms(category, term ASC) WHERE is_active = true;
CREATE INDEX idx_user_lesson_progress_user ON learn.user_lesson_progress(user_id, status, last_viewed_at DESC);
CREATE INDEX idx_quiz_attempts_user_lesson ON learn.quiz_attempts(user_id, lesson_id, attempt_number DESC);
CREATE INDEX idx_orders_open_paper ON practice.orders(symbol, price) WHERE status = 'OPEN' AND is_paper = true;
CREATE INDEX idx_trades_order_id ON practice.trades(order_id);
CREATE INDEX idx_ledger_user_id ON practice.ledger(user_id, created_at DESC);
CREATE INDEX idx_holdings_user ON practice.holdings(user_id, is_paper);
CREATE INDEX idx_price_alerts_active ON practice.price_alerts(symbol, status) WHERE status = 'ACTIVE';
CREATE INDEX idx_portfolio_insights_user_date ON profile.portfolio_insights(user_id, insight_date DESC, language);
CREATE INDEX idx_user_features_churn ON profile.user_features(churn_score DESC, churn_score_computed_at) WHERE churn_score > 0.5;
CREATE INDEX idx_audit_log_user_id ON public.audit_log(user_id, created_at DESC) WHERE user_id IS NOT NULL;

-- Search text indexing for symbol searches
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_symbols_company_name_trgm ON market.symbols USING gin(company_name gin_trgm_ops);
CREATE INDEX idx_symbols_short_name_trgm ON market.symbols USING gin(short_name gin_trgm_ops);
CREATE INDEX idx_symbols_exchange_type ON market.symbols(exchange, instrument_type) WHERE is_active = true;
CREATE INDEX idx_symbols_sector ON market.symbols(sector, market_cap_category) WHERE is_active = true AND instrument_type = 'EQ';
CREATE INDEX idx_symbols_fo_enabled ON market.symbols(is_fo_enabled) WHERE is_fo_enabled = true AND is_active = true;
CREATE INDEX idx_symbols_isin ON market.symbols(isin) WHERE isin IS NOT NULL;
CREATE INDEX idx_symbols_fyers ON market.symbols(fyers_symbol) WHERE fyers_symbol IS NOT NULL;
CREATE INDEX idx_symbols_suspended ON market.symbols(is_suspended) WHERE is_suspended = true;

-- Additional Indexes for exchange holidays and listings
CREATE INDEX idx_exchange_holidays_date ON market.exchange_holidays(holiday_date, exchange) WHERE is_trading_day = false;
CREATE INDEX idx_exchange_holidays_upcoming ON market.exchange_holidays(holiday_date ASC) WHERE holiday_date >= CURRENT_DATE;
CREATE INDEX idx_portfolio_insights_fallback ON profile.portfolio_insights(insight_date, generation_status) WHERE generation_status != 'GENERATED';
CREATE INDEX idx_user_features_conversion_ready ON profile.user_features(paper_trades_total, has_real_investment) WHERE has_real_investment = false AND paper_trades_total >= 5;
CREATE INDEX idx_user_features_inactive ON profile.user_features(days_since_last_active, streak_days_current) WHERE days_since_last_active BETWEEN 1 AND 7;
CREATE INDEX idx_audit_log_failures ON public.audit_log(action, result, created_at DESC) WHERE result = 'FAILURE';
CREATE INDEX idx_audit_log_created_at ON public.audit_log(created_at DESC);

-- -----------------------------------------------------
-- 8. SEED DATA FOR BADGES & VOCABULARY JARGONS
-- -----------------------------------------------------
INSERT INTO profile.badge_definitions
  (badge_key, category, name_en, name_hi, description_en, description_hi, icon_emoji, xp_bonus, unlock_criteria_en, sort_order)
VALUES
  ('FIRST_LESSON', 'LEARNING', 'First Step', 'पहला कदम', 'Completed your very first lesson', 'पहला lesson पूरा किया', '📚', 10, 'Complete any 1 lesson', 1),
  ('CHAPTER_COMPLETE', 'LEARNING', 'Chapter Champion', 'चैप्टर चैंपियन', 'Completed an entire chapter', 'पूरा chapter पूरा किया', '📚', 100, 'Complete all lessons in any chapter', 2),
  ('PERFECT_QUIZ', 'LEARNING', 'Quiz Master', 'क्विज़ मास्टर', 'Scored 100% on a quiz', 'Quiz में 100% score किया', '📚', 50, 'Score 100% on any quiz on first attempt', 3),
  ('ALL_LESSONS', 'LEARNING', 'Graduate', 'ग्रेजुएट', 'Completed every single lesson', 'सभी lessons पूरे किए', '📚', 500, 'Complete all available lessons', 4),
  ('JARGON_BUSTER', 'LEARNING', 'Word Wizard', 'वर्ड विज़ार्ड', 'Tapped 50 jargon terms to learn their meaning', '50 जार्गन terms सीखे', '📚', 25, 'Tap 50 different jargon terms', 5),
  ('STREAK_7', 'STREAK', '7-Day Streak', '7 दिन की streak', 'Learned 7 days in a row', 'लगातार 7 दिन सीखा', '📚', 50, 'Maintain a 7-day learning streak', 10),
  ('STREAK_30', 'STREAK', '30-Day Streak', '30 दिन की streak', 'Incredible! 30 days of consistent learning', 'शानदार! 30 दिन लगातार सीखा', '⚡', 200, 'Maintain a 30-day learning streak', 11),
  ('STREAK_100', 'STREAK', '100-Day Streak', '100 दिन की streak', 'Legendary dedication. 100 days!', 'अविश्वसनीय! 100 दिन!', '📚', 1000, 'Maintain a 100-day learning streak', 12),
  ('FIRST_PAPER_TRADE', 'TRADING', 'Practice Makes Perfect', 'प्रैक्टिस से परफेक्ट', 'Placed your first practice trade', 'पहला practice trade किया', '📚', 25, 'Place any 1 paper trade', 20),
  ('PAPER_PROFIT', 'TRADING', 'Green Zone', 'ग्रीन ज़ोन', 'Made your practice portfolio profitable', 'Practice portfolio को प्रॉफिटेबल बनाया', '📚', 50, 'Achieve positive P&L in paper trading', 21),
  ('FIRST_REAL_TRADE', 'TRADING', 'Real Investor', 'असली निवेशक', 'Placed your first real stock order', 'पहला real order लगाया', '📚', 100, 'Place any 1 real stock order after KYC', 22),
  ('FIRST_SIP', 'TRADING', 'SIP Starter', 'SIP शुरू की', 'Started your first SIP', 'पहली SIP शुरू की', '📚', 50, 'Set up any SIP for any amount', 23),
  ('FIRST_MF', 'TRADING', 'Fund Investor', 'फंड निवेशक', 'Made your first mutual fund investment', 'पहला Mutual Fund investment किया', '📚', 75, 'Make any lumpsum MF investment', 24),
  ('FIRST_POST', 'COMMUNITY', 'Question Starter', 'पहला सवाल पूछा', 'Asked your first community question', 'पहला community question पूछा', '📚', 10, 'Post any 1 question in community', 30),
  ('FIRST_ANSWER', 'COMMUNITY', 'Helper', 'मददगार', 'Answered someone''s question', 'किसी के सवाल का जवाब दिया', '📚', 15, 'Post any 1 answer in community', 31),
  ('VERIFIED_HELPER', 'COMMUNITY', 'Verified Helper', 'Verified Helper', 'Earned the Verified Helper badge for expertise', 'expertise के लिए Verified Helper बैज', '✅', 200, 'Complete all lessons with >70% quiz pass rate', 32),
  ('KYC_COMPLETE', 'MILESTONE', 'Identity Verified', 'KYC पूर्ण', 'Completed your KYC verification', 'KYC verification पूरा हुआ', '📚', 0, 'Complete KYC verification', 40),
  ('LEVEL_5', 'MILESTONE', 'Level 5 Achiever', 'Level 5 अचीवर', 'Reached Level 5 - Real investing unlocked!', 'Level 5 अनलॉक - Real investing unlock!', '⭐', 0, 'Accumulate 2000 XP', 41),
  ('LEVEL_10', 'MILESTONE', 'Master Investor', 'मास्टर निवेशक', 'Reached the highest level!', 'सबसे उच्च level पर पहुंचे!', '📚', 500, 'Accumulate 5000 XP', 42),
  ('NIGHT_OWL', 'SPECIAL', 'Night Owl', 'रात का उल्लू', 'Completed a lesson after midnight', 'रात 12 बजे के बाद lesson पूरा', '📚', 20, 'Complete any lesson between 12 AM and 5 AM', 50),
  ('SPEED_LEARNER', 'SPECIAL', 'Speed Learner', 'तेज़ सीखने वाला', 'Completed 3 lessons in one day', 'एक दिन में 3 lessons पूरे', '⚡', 30, 'Complete 3 lessons in a single day', 51);

INSERT INTO learn.jargon_terms
  (term, term_display, category, definition_en, definition_hi, analogy_hi, example_en, related_terms)
VALUES
  ('nav', 'NAV', 'MUTUAL_FUNDS', 'Net Asset Value. The price per unit of a mutual fund.', 'NAV यानी Net Asset Value। म्यूचुअल फंड के एक यूनिट का मूल्य।', 'मान लो 100 करोड़ का फंड है और 10 करोड़ यूनिट्स हैं तो NAV 10 रुपये प्रति यूनिट होगी।', 'Fund total assets = ₹10 crore, Total units = 10 lakh. NAV = ₹100 per unit.', ARRAY['aum', 'units', 'expense_ratio']),
  ('sip', 'SIP', 'MUTUAL_FUNDS', 'Systematic Investment Plan. A method of investing a fixed amount in a mutual fund.', 'SIP यानी Systematic Investment Plan। हर महीने या तय समय पर म्यूचुअल फंड में नियमित निवेश।', 'जैसे हर महीने गुल्लक में पैसे डालना वैसे ही हर महीने तय तारीख पर म्यूचुअल फंड में निवेश।', 'Invest ₹1,00,000 per month in a fund for 5 years.', ARRAY['nav', 'rupee_cost_averaging', 'mutual_fund']),
  ('expense_ratio', 'Expense Ratio', 'MUTUAL_FUNDS', 'The annual fee charged by a mutual fund to manage your money.', 'Expense Ratio वह वार्षिक फीस है जो म्यूचुअल फंड आपके पैसों को मैनेज करने के लिए लेता है।', 'जैसे प्रॉपर्टी मैनेजर को किराया संभालने के बदले फीस दी जाती है वैसे ही फंड मैनेजर को यह फीस मिलती है।', 'Direct plans have ~0.5% expense ratio vs Regular plans at ~1.5%.', ARRAY['nav', 'direct_plan', 'regular_plan', 'amc']);
