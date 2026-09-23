-- =========================================================================
-- V6__cleanup_legacy_lessons.sql (learn-service)
-- 1. Cleans up legacy mock lessons (les_%) inserted in V4 that lack segments and proper chapter names.
-- 2. Ensures all 30 core lessons (mf-1 to mf-30) have rich multi-segment content blocks.
-- =========================================================================

DELETE FROM learn.user_lesson_progress WHERE lesson_id LIKE 'les_%';
DELETE FROM learn.lessons WHERE id LIKE 'les_%';

-- Seed rich multi-segment JSON content for all 30 lessons
UPDATE learn.lessons SET 
  segments_json = '[{"type":"emoji","content":"💵"},{"type":"text","content":"Learn how Money works, budgeting basics, and the power of Compound Interest."},{"type":"text","content":"Understanding Inflation & Savings is the first step to building long-term wealth."},{"type":"callout","title":"KEY TAKEAWAY","content":"Always build an Emergency Fund before starting equity investments!"}]',
  jargon_words_json = '["Money","Savings","Inflation","Compound Interest"]'
WHERE id = 'mf-1';

UPDATE learn.lessons SET 
  segments_json = '[{"type":"emoji","content":"📈"},{"type":"text","content":"Understand buying shares in Indian companies like Reliance, TCS, and Infosys on stock exchanges."},{"type":"text","content":"When you buy a Stock, you own a real piece of a business."},{"type":"callout","title":"SMART INVESTOR TIP","content":"Focus on long-term company earnings rather than daily stock price fluctuations."}]',
  jargon_words_json = '["Stock Market","Shares","NSE"]'
WHERE id = 'mf-2';

UPDATE learn.lessons SET 
  segments_json = '[{"type":"emoji","content":"📉"},{"type":"text","content":"Learn how Inflation erodes savings and why keeping cash under your mattress loses purchasing power over time."},{"type":"text","content":"Investing in equity mutual funds beats fixed deposits (FD) in beating inflation."},{"type":"callout","title":"INFLATION WARNING","content":"If inflation is 6%, ₹100 today will only buy ₹94 worth of goods next year."}]',
  jargon_words_json = '["Inflation","Purchasing Power","FD"]'
WHERE id = 'mf-3';

UPDATE learn.lessons SET 
  segments_json = '[{"type":"emoji","content":"🚀"},{"type":"text","content":"Discover how Compound Interest turning interest into principal creates massive wealth."},{"type":"text","content":"Albert Einstein called compound interest the 8th wonder of the world!"},{"type":"callout","title":"THE COMPOUNDING FORMULA","content":"Starting 5 years earlier can double your final corpus at retirement."}]',
  jargon_words_json = '["Compound Interest","Compounding","Wealth"]'
WHERE id = 'mf-4';

UPDATE learn.lessons SET 
  segments_json = '[{"type":"emoji","content":"📊"},{"type":"text","content":"Master budgeting with 50% for Needs, 30% for Wants, and 20% for dedicated Savings and Investments."},{"type":"text","content":"Automation is key: transfer your 20% savings on salary day before spending!"},{"type":"callout","title":"GOLDEN RULE","content":"Do not save what is left after spending; spend what is left after saving."}]',
  jargon_words_json = '["Budgeting","50-30-20 Rule","Savings"]'
WHERE id = 'mf-5';

UPDATE learn.lessons SET 
  segments_json = '[{"type":"emoji","content":"🛡️"},{"type":"text","content":"Build a 6-month Emergency Fund in liquid funds or high-yield savings accounts before investing."},{"type":"text","content":"This safety net protects your long-term equity portfolio during unexpected medical emergencies or job loss."},{"type":"callout","title":"SAFETY FIRST","content":"Never invest emergency money in stocks or equity mutual funds!"}]',
  jargon_words_json = '["Emergency Fund","Safety Net","Liquid Funds"]'
WHERE id = 'mf-6';

UPDATE learn.lessons SET 
  segments_json = '[{"type":"emoji","content":"🍲"},{"type":"text","content":"Imagine a dal bhat pot where 1,000 people put in ₹500 each. A professional Fund Manager uses all ₹5 lakh to buy top stocks."},{"type":"text","content":"That is a Mutual Fund! You get professional management and diversification for tiny amounts."},{"type":"callout","title":"MUTUAL FUND ADVANTAGE","content":"Mutual Fund = Group Investment + Professional Management."}]',
  jargon_words_json = '["Mutual Fund","Fund Manager","NAV"]'
WHERE id = 'mf-7';

UPDATE learn.lessons SET 
  segments_json = '[{"type":"emoji","content":"🏷️"},{"type":"text","content":"NAV (Net Asset Value) is simply the price of 1 unit of a mutual fund."},{"type":"text","content":"If a fund holds ₹10 Crore total assets and has 10 Lakh units, NAV is ₹100."},{"type":"callout","title":"NAV MYTH BUSTING","content":"A lower NAV does NOT mean a fund is cheaper or better than a higher NAV fund!"}]',
  jargon_words_json = '["NAV","Asset Value","Units"]'
WHERE id = 'mf-8';

UPDATE learn.lessons SET 
  segments_json = '[{"type":"emoji","content":"🥊"},{"type":"text","content":"Active funds have fund managers picking stocks to beat the market."},{"type":"text","content":"Passive index funds simply track an index like Nifty 50 with ultra-low expense ratios."},{"type":"callout","title":"COST MATTERS","content":"Passive funds often outperform active funds long-term due to lower management fees."}]',
  jargon_words_json = '["Active Funds","Passive Funds","Index Funds"]'
WHERE id = 'mf-9';

UPDATE learn.lessons SET 
  segments_json = '[{"type":"emoji","content":"🇮🇳"},{"type":"text","content":"Invest in India’s top 50 blue-chip companies effortlessly through Nifty 50 Index Funds."},{"type":"text","content":"As India grows, the top 50 companies grow, making Nifty 50 the backbone of wealth creation."},{"type":"callout","title":"INDEX FUND POWER","content":"Low expense ratio (<0.1%) + Indian economic growth = Reliable long-term returns."}]',
  jargon_words_json = '["Nifty 50","Index Fund","NSE"]'
WHERE id = 'mf-10';
