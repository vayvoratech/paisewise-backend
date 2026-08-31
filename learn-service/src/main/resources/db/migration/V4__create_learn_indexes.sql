-- V4__create_learn_indexes.sql
CREATE INDEX IF NOT EXISTS idx_lessons_difficulty ON learn.lessons(difficulty);
CREATE INDEX IF NOT EXISTS idx_user_lesson_progress_user_id ON learn.user_lesson_progress(user_id);

INSERT INTO learn.lessons (id, title, description, duration_minutes, xp_reward, difficulty) VALUES
                                                                                                ('les_00000000-0000-0000-0000-000000000001', 'Introduction to Stock Markets', 'Learn the absolute basics of how stock exchanges work and what shares represent.', 10, 100, 'BEGINNER'),
                                                                                                ('les_00000000-0000-0000-0000-000000000002', 'Understanding Market vs Limit Orders', 'Deep dive into execution types and how to control your entry price.', 15, 150, 'BEGINNER'),
                                                                                                ('les_00000000-0000-0000-0000-000000000003', 'Reading Candlestick Charts', 'Recognize common bullish and bearish candlestick patterns.', 20, 200, 'INTERMEDIATE'),
                                                                                                ('les_00000000-0000-0000-0000-000000000004', 'Risk Management & Position Sizing', 'Calculate risk per trade to protect your capital effectively.', 25, 250, 'INTERMEDIATE'),
                                                                                                ('les_00000000-0000-0000-0000-000000000005', 'Introduction to Derivatives (F&O)', 'Understand Futures and Options contracts and their primary use cases.', 30, 300, 'ADVANCED'),
                                                                                                ('les_00000000-0000-0000-0000-000000000006', 'P/E Ratios and Fundamental Analysis', 'Evaluate company financial health using valuation metrics.', 20, 200, 'INTERMEDIATE'),
                                                                                                ('les_00000000-0000-0000-0000-000000000007', 'Stop Loss Strategies', 'Protect open positions against volatile market reversals.', 15, 150, 'BEGINNER'),
                                                                                                ('les_00000000-0000-0000-0000-000000000008', 'Sector Rotation and Trends', 'Learn how capital moves across different economic sectors.', 20, 200, 'INTERMEDIATE'),
                                                                                                ('les_00000000-0000-0000-0000-000000000009', 'Portfolio Diversification', 'Avoid concentration risk by balancing asset allocations properly.', 15, 150, 'BEGINNER'),
                                                                                                ('les_00000000-0000-0000-0000-000000000010', 'Trading Psychology and Discipline', 'Master your emotions to prevent impulsive decision-making.', 25, 300, 'ADVANCED')
    ON CONFLICT (id) DO NOTHING;