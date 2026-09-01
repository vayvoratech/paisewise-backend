-- =========================================================================
-- V5__seed_lessons.sql (learn-service)
-- Seeds 10 financial literacy lessons matching platform curriculum specs
-- =========================================================================
INSERT INTO learn.lessons (id, title, chapter, chapter_no, index, total, quiz_xp, segments_json, jargon_words_json) VALUES
('mf-1', 'Introduction to Money & Savings', 'Financial Basics', 1, 1, 10, 50, '[{"type":"text","content":"Learn how money works, budgeting basics, and the power of compound interest."}]', '["Money","Savings","Inflation"]'),
('mf-2', 'What is the Stock Market?', 'Financial Basics', 1, 2, 10, 50, '[{"type":"text","content":"Understand buying shares in Indian companies like Reliance and TCS."}]', '["Stock Market","Shares","NSE"]'),
('mf-3', 'What is a Mutual Fund?', 'Mutual Funds', 2, 1, 10, 50, '[{"type":"text","content":"Group investment managed by professional fund managers."}]', '["Mutual Fund","Fund Manager","NAV"]'),
('mf-4', 'Understanding NAV (Net Asset Value)', 'Mutual Funds', 2, 2, 10, 50, '[{"type":"text","content":"NAV is the price of one unit of a mutual fund."}]', '["NAV","Asset Value","Units"]'),
('mf-5', 'SIP vs Lumpsum Investment', 'Investing Strategies', 3, 1, 10, 50, '[{"type":"text","content":"Systematic Investment Plan (SIP) helps average out market volatility."}]', '["SIP","Lumpsum","Dollar Cost Averaging"]'),
('mf-6', 'Equity vs Debt Funds', 'Asset Allocation', 3, 2, 10, 50, '[{"type":"text","content":"Equity funds invest in stocks, debt funds invest in bonds and government securities."}]', '["Equity","Debt","Bonds"]'),
('mf-7', 'Risk & Return Trade-off', 'Risk Management', 4, 1, 10, 50, '[{"type":"text","content":"Higher returns usually come with higher volatility."}]', '["Risk","Volatility","CAGR"]'),
('mf-8', 'Expense Ratio & Exit Load', 'Fund Costs', 4, 2, 10, 50, '[{"type":"text","content":"Expense ratio is the annual fee charged by mutual funds to manage your money."}]', '["Expense Ratio","Exit Load","TER"]'),
('mf-9', 'Tax Implications of Mutual Funds', 'Taxation', 5, 1, 10, 50, '[{"type":"text","content":"STCG and LTCG taxes apply when selling mutual fund units."}]', '["LTCG","STCG","ELSS"]'),
('mf-10', 'Building Your First Wealth Portfolio', 'Portfolio Construction', 5, 2, 10, 50, '[{"type":"text","content":"Diversify across large cap, mid cap, and debt funds for financial freedom."}]', '["Portfolio","Diversification","Financial Goal"]')
ON CONFLICT (id) DO NOTHING;
