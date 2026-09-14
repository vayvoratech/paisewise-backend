-- =========================================================================
-- V5__seed_lessons.sql (learn-service)
-- Seeds 30 financial literacy lessons across 5 chapters with JSONB content blocks
-- =========================================================================
INSERT INTO learn.lessons (id, title, chapter, chapter_no, index, total, quiz_xp, segments_json, jargon_words_json) VALUES
('mf-1', 'Introduction to Money & Savings', 'Financial Basics', 1, 1, 6, 50, '[{"type":"text","content":"Learn how money works, budgeting basics, and the power of compound interest."}]', '["Money","Savings","Inflation"]'),
('mf-2', 'What is the Stock Market?', 'Financial Basics', 1, 2, 6, 50, '[{"type":"text","content":"Understand buying shares in Indian companies like Reliance and TCS."}]', '["Stock Market","Shares","NSE"]'),
('mf-3', 'Understanding Inflation & Purchasing Power', 'Financial Basics', 1, 3, 6, 50, '[{"type":"text","content":"Learn how inflation erodes savings and why investing beats fixed deposits."}]', '["Inflation","Purchasing Power","FD"]'),
('mf-4', 'The Magic of Compound Interest', 'Financial Basics', 1, 4, 6, 50, '[{"type":"text","content":"Discover how interest earning interest turns small monthly savings into wealth."}]', '["Compound Interest","Compounding","Wealth"]'),
('mf-5', 'Needs vs Wants: The 50/30/20 Rule', 'Financial Basics', 1, 5, 6, 50, '[{"type":"text","content":"Master budgeting with 50% needs, 30% wants, and 20% dedicated savings."}]', '["Budgeting","50-30-20 Rule","Savings"]'),
('mf-6', 'Emergency Funds & Financial Safety', 'Financial Basics', 1, 6, 6, 50, '[{"type":"text","content":"Build a 6-month safety net before starting your investment journey."}]', '["Emergency Fund","Safety Net","Liquid Funds"]'),
('mf-7', 'What is a Mutual Fund?', 'Mutual Funds & NAV', 2, 1, 6, 50, '[{"type":"text","content":"Group investment managed by professional fund managers."}]', '["Mutual Fund","Fund Manager","NAV"]'),
('mf-8', 'Understanding NAV (Net Asset Value)', 'Mutual Funds & NAV', 2, 2, 6, 50, '[{"type":"text","content":"NAV is the price of one unit of a mutual fund."}]', '["NAV","Asset Value","Units"]'),
('mf-9', 'Active vs Passive Mutual Funds', 'Mutual Funds & NAV', 2, 3, 6, 50, '[{"type":"text","content":"Compare fund manager stock picking vs low-cost market index tracking."}]', '["Active Funds","Passive Funds","Index Funds"]'),
('mf-10', 'Index Funds & Nifty 50 Investing', 'Mutual Funds & NAV', 2, 4, 6, 50, '[{"type":"text","content":"Invest in top 50 Indian companies with ultra-low expense ratios."}]', '["Nifty 50","Index Fund","NSE"]'),
('mf-11', 'Large Cap, Mid Cap & Small Cap Funds', 'Mutual Funds & NAV', 2, 5, 6, 50, '[{"type":"text","content":"Explore company market capitalization categories and market risk levels."}]', '["Large Cap","Mid Cap","Small Cap"]'),
('mf-12', 'Sectoral & Thematic Mutual Funds', 'Mutual Funds & NAV', 2, 6, 6, 50, '[{"type":"text","content":"Invest in specific industries like IT, Pharma, Banking, and EV energy."}]', '["Sectoral","Thematic","Concentration Risk"]'),
('mf-13', 'SIP vs Lumpsum Investment', 'Investing Strategies & SIPs', 3, 1, 6, 50, '[{"type":"text","content":"Systematic Investment Plan (SIP) helps average out market volatility."}]', '["SIP","Lumpsum","Dollar Cost Averaging"]'),
('mf-14', 'Step-Up SIP: Scaling Your Wealth', 'Investing Strategies & SIPs', 3, 2, 6, 50, '[{"type":"text","content":"Increase your SIP amount by 10% annually to accelerate financial goals."}]', '["Step-Up SIP","Annual Increase","Compounding"]'),
('mf-15', 'Rupee Cost Averaging Explained', 'Investing Strategies & SIPs', 3, 3, 6, 50, '[{"type":"text","content":"Buy more units when prices dip and fewer when markets rally."}]', '["Rupee Cost Averaging","Volatility","Market Dip"]'),
('mf-16', 'Goal-Based Investing', 'Investing Strategies & SIPs', 3, 4, 6, 50, '[{"type":"text","content":"Map your investments to house purchases, children education, and retirement."}]', '["Goal-Based","Time Horizon","Target Corpus"]'),
('mf-17', 'Market Timing vs Time in Market', 'Investing Strategies & SIPs', 3, 5, 6, 50, '[{"type":"text","content":"Why consistent long-term holding always beats predicting daily market highs and lows."}]', '["Market Timing","Long-Term","Consistency"]'),
('mf-18', 'Systematic Withdrawal Plans (SWP)', 'Investing Strategies & SIPs', 3, 6, 6, 50, '[{"type":"text","content":"Generate steady monthly income during retirement using SWP."}]', '["SWP","Retirement Income","Cash Flow"]'),
('mf-19', 'Equity vs Debt Funds', 'Asset Allocation & Risk Management', 4, 1, 6, 50, '[{"type":"text","content":"Equity funds invest in stocks, debt funds invest in bonds and government securities."}]', '["Equity","Debt","Bonds"]'),
('mf-20', 'Risk & Return Trade-off', 'Asset Allocation & Risk Management', 4, 2, 6, 50, '[{"type":"text","content":"Higher returns usually come with higher volatility."}]', '["Risk","Volatility","CAGR"]'),
('mf-21', 'Expense Ratio & Exit Load', 'Asset Allocation & Risk Management', 4, 3, 6, 50, '[{"type":"text","content":"Expense ratio is the annual fee charged by mutual funds to manage your money."}]', '["Expense Ratio","Exit Load","TER"]'),
('mf-22', 'Understanding CAGR & XIRR', 'Asset Allocation & Risk Management', 4, 4, 6, 50, '[{"type":"text","content":"Calculate annual compound returns (CAGR) and cashflow returns (XIRR)."}]', '["CAGR","XIRR","Return Metrics"]'),
('mf-23', 'Managing Volatility & Market Crashes', 'Asset Allocation & Risk Management', 4, 5, 6, 50, '[{"type":"text","content":"Stay calm during market corrections and take advantage of discounted unit prices."}]', '["Volatility","Market Crash","Correction"]'),
('mf-24', 'Diversification: Don''t Put All Eggs in One Basket', 'Asset Allocation & Risk Management', 4, 6, 6, 50, '[{"type":"text","content":"Spread investments across equity, gold, fixed income, and international markets."}]', '["Diversification","Asset Class","Risk Reduction"]'),
('mf-25', 'Tax Implications of Equity & Debt Funds', 'Taxation & Portfolio Construction', 5, 1, 6, 50, '[{"type":"text","content":"STCG and LTCG taxes apply when selling mutual fund units."}]', '["LTCG","STCG","Tax Slabs"]'),
('mf-26', 'ELSS: Saving Income Tax Under 80C', 'Taxation & Portfolio Construction', 5, 2, 6, 50, '[{"type":"text","content":"Save up to ₹46,800 tax under Section 80C with 3-year lock-in ELSS funds."}]', '["ELSS","Section 80C","Tax Saver"]'),
('mf-27', 'Short-Term vs Long-Term Capital Gains', 'Taxation & Portfolio Construction', 5, 3, 6, 50, '[{"type":"text","content":"Understand 12-month holding thresholds for equity tax treatment."}]', '["Holding Period","LTCG Exemptions","Capital Gains"]'),
('mf-28', 'Rebalancing Your Investment Portfolio', 'Taxation & Portfolio Construction', 5, 4, 6, 50, '[{"type":"text","content":"Reset your asset allocation annually to lock in profits and manage risk."}]', '["Rebalancing","Asset Allocation","Profit Booking"]'),
('mf-29', 'Common Behavioral Pitfalls of Investors', 'Taxation & Portfolio Construction', 5, 5, 6, 50, '[{"type":"text","content":"Avoid panic selling, FOMO buying, and chasing last year''s top performers."}]', '["Behavioral Finance","FOMO","Panic Selling"]'),
('mf-30', 'Building Your First Wealth Portfolio', 'Taxation & Portfolio Construction', 5, 6, 6, 50, '[{"type":"text","content":"Diversify across large cap, mid cap, and debt funds for financial freedom."}]', '["Portfolio","Diversification","Financial Freedom"]')
ON CONFLICT (id) DO UPDATE SET
  title = EXCLUDED.title,
  chapter = EXCLUDED.chapter,
  chapter_no = EXCLUDED.chapter_no,
  index = EXCLUDED.index,
  total = EXCLUDED.total,
  quiz_xp = EXCLUDED.quiz_xp,
  segments_json = EXCLUDED.segments_json,
  jargon_words_json = EXCLUDED.jargon_words_json;

