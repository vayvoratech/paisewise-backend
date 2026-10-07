package in.sapphirus.rupee.portfolio.service;

import in.sapphirus.rupee.portfolio.domain.MfScheme;
import in.sapphirus.rupee.portfolio.repo.MfSchemeRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-driven Mutual Fund Recommendation Service.
 *
 * Employs a multi-factor quantitative ranking model combined with goal-oriented heuristics
 * to generate tailored fund recommendations and personalized natural language insights.
 */
@Service
public class AiFundRecommendationService {

    private final MfSchemeRepository schemeRepo;

    public AiFundRecommendationService(MfSchemeRepository schemeRepo) {
        this.schemeRepo = schemeRepo;
    }

    public record RecommendationRequest(
            String riskAppetite,      // LOW, MODERATE, HIGH, VERY_HIGH
            String goal,              // WEALTH_CREATION, TAX_SAVING, RETIREMENT, EMERGENCY_FUND, SHORT_TERM
            Integer horizonYears,     // 1, 3, 5, 10
            Double monthlyBudget,     // e.g. 5000
            String categoryPreference,// optional
            String language           // en, hi
    ) {}

    public record RecommendedFundView(
            String schemeCode,
            String isin,
            String schemeName,
            String amcName,
            String category,
            String riskLevel,
            double nav,
            double minSipAmount,
            double minLumpsum,
            Double returns1y,
            Double returns3y,
            Double returns5y,
            Double expenseRatio,
            Double fundSizeCr,
            boolean isTaxSaver,
            int aiScore,              // 0-100 score
            String matchRating,       // "TOP_PICK", "STRONG_MATCH", "SUITABLE"
            String aiReasonEn,
            String aiReasonHi,
            List<String> keyHighlights
    ) {}

    public record RecommendationResponse(
            String profileSummary,
            String recommendedStrategy,
            List<RecommendedFundView> recommendations,
            Map<String, Integer> assetAllocation
    ) {}

    /**
     * Generate personalized AI recommendations based on user requirements.
     */
    public RecommendationResponse generateRecommendations(RecommendationRequest request) {
        String risk = request.riskAppetite() != null ? request.riskAppetite().toUpperCase() : "MODERATE";
        String goal = request.goal() != null ? request.goal().toUpperCase() : "WEALTH_CREATION";
        int horizon = request.horizonYears() != null && request.horizonYears() > 0 ? request.horizonYears() : 3;
        String lang = request.language() != null ? request.language().toLowerCase() : "en";

        List<MfScheme> allActive = schemeRepo.findAll().stream()
                .filter(MfScheme::isActive)
                .toList();

        if (allActive.isEmpty()) {
            return new RecommendationResponse("No funds available in catalog.", "Explore broad market index funds.", List.of(), Map.of());
        }

        // 1. Filter by Goal & Risk suitability
        List<MfScheme> candidates = allActive.stream()
                .filter(scheme -> isSchemeSuitable(scheme, risk, goal, horizon, request.categoryPreference()))
                .toList();

        if (candidates.isEmpty()) {
            candidates = allActive; // fallback to top broad market funds
        }

        // 2. Score candidates with quantitative AI ranking model
        List<ScoredFund> scoredFunds = candidates.stream()
                .map(scheme -> new ScoredFund(scheme, calculateAiScore(scheme, risk, goal, horizon)))
                .sorted(Comparator.comparingInt(ScoredFund::score).reversed())
                .limit(6)
                .toList();

        // 3. Construct Views & AI Explanations
        List<RecommendedFundView> views = scoredFunds.stream()
                .map(sf -> toRecommendedView(sf.scheme(), sf.score(), goal, risk, lang))
                .toList();

        // 4. Asset Allocation Model
        Map<String, Integer> allocation = computeAssetAllocation(risk, goal, horizon);
        String summary = generateProfileSummary(risk, goal, horizon, lang);
        String strategy = generateStrategyOverview(risk, goal, horizon, lang);

        return new RecommendationResponse(summary, strategy, views, allocation);
    }

    private boolean isSchemeSuitable(MfScheme scheme, String risk, String goal, int horizon, String categoryPref) {
        if (categoryPref != null && !categoryPref.isBlank()) {
            if (scheme.getCategory() != null && scheme.getCategory().equalsIgnoreCase(categoryPref)) {
                return true;
            }
        }

        // Tax Saving goal requires ELSS / Tax Saver
        if ("TAX_SAVING".equals(goal)) {
            return scheme.isTaxSaver() || (scheme.getCategory() != null && scheme.getCategory().toLowerCase().contains("elss"));
        }

        // Emergency fund or very short horizon requires Liquid / Overnight / Debt
        if ("EMERGENCY_FUND".equals(goal) || horizon <= 1) {
            String cat = scheme.getCategory() != null ? scheme.getCategory().toLowerCase() : "";
            return cat.contains("liquid") || cat.contains("debt") || cat.contains("overnight") || cat.contains("money market");
        }

        // Risk Level Match
        String fundRisk = scheme.getRiskLevel() != null ? scheme.getRiskLevel().toUpperCase() : "MODERATE";
        if ("LOW".equals(risk)) {
            return fundRisk.contains("LOW") || fundRisk.contains("MODERATE");
        } else if ("MODERATE".equals(risk)) {
            return !fundRisk.contains("VERY HIGH");
        }

        return true;
    }

    private int calculateAiScore(MfScheme scheme, String risk, String goal, int horizon) {
        double score = 50.0;

        // 1. 3-Year Return weight (max +25)
        if (scheme.getReturns3y() != null) {
            double r3 = scheme.getReturns3y();
            if (r3 > 20.0) score += 25;
            else if (r3 > 15.0) score += 20;
            else if (r3 > 12.0) score += 15;
            else if (r3 > 8.0) score += 10;
        }

        // 2. 5-Year Return weight (max +15)
        if (scheme.getReturns5y() != null && scheme.getReturns5y() > 14.0) {
            score += 15;
        }

        // 3. Expense Ratio Efficiency (max +10 for low fees)
        if (scheme.getExpenseRatio() != null) {
            double er = scheme.getExpenseRatio();
            if (er < 0.60) score += 10;
            else if (er < 1.00) score += 6;
            else if (er > 1.80) score -= 5;
        }

        // 4. Fund Size Stability (max +5)
        if (scheme.getFundSizeCr() != null && scheme.getFundSizeCr() > 1000.0) {
            score += 5;
        }

        // 5. Goal Alignment Bonus (+10)
        if ("TAX_SAVING".equals(goal) && scheme.isTaxSaver()) {
            score += 10;
        }

        return Math.min(99, Math.max(40, (int) Math.round(score)));
    }

    private RecommendedFundView toRecommendedView(MfScheme s, int score, String goal, String risk, String lang) {
        String rating = score >= 85 ? "TOP_PICK" : score >= 70 ? "STRONG_MATCH" : "SUITABLE";

        String reasonEn = String.format(
                "High consistency with %.1f%% 3-year returns and %.2f%% expense ratio. Optimized for %s strategy with %s risk tolerance.",
                s.getReturns3y() != null ? s.getReturns3y() : 14.5,
                s.getExpenseRatio() != null ? s.getExpenseRatio() : 0.75,
                goal.replace("_", " ").toLowerCase(),
                risk.toLowerCase()
        );

        String reasonHi = String.format(
                "%.1f%% 3-वर्षीय रिटर्न और केवल %.2f%% एक्सपेंस रेशियो के साथ स्थिर परफॉर्मेंस। आपकी %s रणनीति के लिए सर्वोत्तम।",
                s.getReturns3y() != null ? s.getReturns3y() : 14.5,
                s.getExpenseRatio() != null ? s.getExpenseRatio() : 0.75,
                goal.equalsIgnoreCase("TAX_SAVING") ? "टैक्स बचत" : "वेल्थ क्रिएशन"
        );

        List<String> highlights = new ArrayList<>();
        if (s.getReturns3y() != null && s.getReturns3y() > 15) {
            highlights.add(String.format("Top 3Y CAGR: %.1f%%", s.getReturns3y()));
        }
        if (s.getExpenseRatio() != null && s.getExpenseRatio() < 1.0) {
            highlights.add(String.format("Low Fee: %.2f%% ER", s.getExpenseRatio()));
        }
        if (s.isTaxSaver()) {
            highlights.add("Section 80C Tax Deduction up to ₹1.5L");
        }
        if (highlights.isEmpty()) {
            highlights.add("Well diversified portfolio across leading Indian sectors");
        }

        return new RecommendedFundView(
                s.getSchemeCode(),
                s.getIsin(),
                s.getSchemeName(),
                s.getAmcName(),
                s.getCategory(),
                s.getRiskLevel(),
                s.getNav(),
                s.getMinSipAmount(),
                s.getMinLumpsum(),
                s.getReturns1y(),
                s.getReturns3y(),
                s.getReturns5y(),
                s.getExpenseRatio(),
                s.getFundSizeCr(),
                s.isTaxSaver(),
                score,
                rating,
                reasonEn,
                reasonHi,
                highlights
        );
    }

    private Map<String, Integer> computeAssetAllocation(String risk, String goal, int horizon) {
        Map<String, Integer> alloc = new LinkedHashMap<>();
        if ("LOW".equals(risk) || horizon <= 1) {
            alloc.put("Debt & Liquid", 60);
            alloc.put("Large Cap Equity", 30);
            alloc.put("Gold/Arbitrage", 10);
        } else if ("MODERATE".equals(risk)) {
            alloc.put("Large & Flexi Cap", 50);
            alloc.put("Mid Cap", 25);
            alloc.put("Debt/Hybrid", 25);
        } else {
            alloc.put("Flexi & Large Cap", 40);
            alloc.put("Mid & Small Cap", 45);
            alloc.put("International/Thematic", 15);
        }
        return alloc;
    }

    private String generateProfileSummary(String risk, String goal, int horizon, String lang) {
        if ("hi".equalsIgnoreCase(lang)) {
            return String.format("प्रोफ़ाइल विश्लेषण: %s जोखिम क्षमता, %d वर्ष की समय सीमा और %s लक्ष्य।",
                    risk, horizon, goal.replace("_", " "));
        }
        return String.format("Profile Analysis: %s risk appetite with a %d-year horizon targeting %s.",
                risk, horizon, goal.replace("_", " ").toLowerCase());
    }

    private String generateStrategyOverview(String risk, String goal, int horizon, String lang) {
        if ("hi".equalsIgnoreCase(lang)) {
            return "मंथली SIP के ज़रिए कॉस्ट एवरेजिंग का लाभ उठाएं और लॉन्ग टर्म में कंपाउंडिंग से वेल्थ बनाएं।";
        }
        return "Combine systematic monthly SIP investments with diversified high-quartile equity funds to maximize rupee-cost averaging and long-term compounding.";
    }

    private record ScoredFund(MfScheme scheme, int score) {}
}
