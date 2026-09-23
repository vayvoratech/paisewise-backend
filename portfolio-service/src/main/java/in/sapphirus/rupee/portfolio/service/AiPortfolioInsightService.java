package in.sapphirus.rupee.portfolio.service;

import in.sapphirus.rupee.portfolio.domain.Holding;
import in.sapphirus.rupee.portfolio.repo.HoldingRepository;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiPortfolioInsightService {

    private final HoldingRepository holdingsRepository;
    private final RestTemplate restTemplate;
    private static final String RENDER_AI_URL = "https://paisewise-aiml-dgd5.onrender.com/ai/portfolio-insight";
    private static final String MARKET_DATA_URL = "http://localhost:8087/market/indices";

    public AiPortfolioInsightService(HoldingRepository holdingsRepository) {
        this.holdingsRepository = holdingsRepository;
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(20000); // 20 seconds
        factory.setReadTimeout(20000);    // 20 seconds
        this.restTemplate = new RestTemplate(factory);
    }

    public record AiHoldingDto(String symbol, int quantity, double avg_buy_price, double current_price) {}
    public record AiMarketContextDto(String market, String trend) {}
    public record AiRequest(String userId, String language, List<AiHoldingDto> holdings, AiMarketContextDto marketContext) {}
    public record AiResponse(String userId, String language, String source, String insight) {}

    public record InsightResult(String insight, String language, String source, String status) {}

    public InsightResult generateInsight(UUID userId, String language) {
        String langCode = resolveLanguage(userId, language);
        List<Holding> userHoldings = holdingsRepository.findByUserId(userId);

        if (userHoldings == null || userHoldings.isEmpty()) {
            String emptyMessage = switch (langCode) {
                case "hi" -> "आपके पोर्टफोलियो में कोई शेयर नहीं मिला। AI पोर्टफोलियो विश्लेषण प्राप्त करने के लिए 'अभ्यास' टैब से अपना पहला शेयर खरीदें!";
                case "te" -> "మీ పోర్ట్‌ఫోలియోలో ఎటువంటి షేర్లు కనుగొనబడలేదు. AI పోర్ట్‌ఫోలియో విశ్లేషణను పొందడానికి ప్రాక్టీస్ ట్యాబ్ నుండి మీ మొదటి షేర్‌ను కొనుగోలు చేయండి!";
                default -> "No stock holdings found in your portfolio. Buy your first practice stock in the Practice tab to get AI-generated insights!";
            };
            return new InsightResult(emptyMessage, langCode, "system", "SUCCESS");
        }

        // 1. Inter-service call to market-data-service to fetch live NIFTY 50 trend & live stock quotes
        String trend = fetchMarketTrend();
        List<String> symbols = userHoldings.stream().map(Holding::getSymbol).toList();
        Map<String, Double> liveQuotes = fetchLiveQuotes(symbols);

        // 2. Prepare AI Request Payload with live market prices (including up & down stocks)
        List<AiHoldingDto> holdingDtos = userHoldings.stream().map(h -> {
            Double livePrice = liveQuotes.get(h.getSymbol());
            double currentPrice = (livePrice != null && livePrice > 0) ? livePrice : (h.getCurrentPrice() > 0 ? h.getCurrentPrice() : h.getAvgPrice());
            return new AiHoldingDto(h.getSymbol(), h.getQuantity(), h.getAvgPrice(), currentPrice);
        }).toList();

        // 3. Dynamic timestamped userId to bypass Render AI server cache when portfolio or language changes!
        String dynamicUserId = (userId != null) ? userId.toString() + "-" + System.currentTimeMillis() : "user_" + System.currentTimeMillis();
        AiMarketContextDto marketContext = new AiMarketContextDto("NIFTY50", trend);
        AiRequest aiReq = new AiRequest(dynamicUserId, langCode, holdingDtos, marketContext);

        // 4. Call Render AI Service with 20s timeout
        try {
            AiResponse resp = restTemplate.postForObject(RENDER_AI_URL, aiReq, AiResponse.class);
            if (resp != null && resp.insight() != null && !resp.insight().isBlank() && !"fallback".equalsIgnoreCase(resp.source())) {
                return new InsightResult(resp.insight(), langCode, resp.source() != null ? resp.source() : "llm", "SUCCESS");
            }
        } catch (Exception e) {
            System.err.println("Render AI Portfolio Insight API call failed: " + e.getMessage());
        }

        // 5. Rich Educational Portfolio Insight Generator (when Render AI is unavailable or returning generic fallback)
        double totalInvested = userHoldings.stream().mapToDouble(h -> h.getAvgPrice() * h.getQuantity()).sum();
        double totalValue = userHoldings.stream().mapToDouble(h -> {
            Double livePrice = liveQuotes.get(h.getSymbol());
            return ((livePrice != null && livePrice > 0) ? livePrice : (h.getCurrentPrice() > 0 ? h.getCurrentPrice() : h.getAvgPrice())) * h.getQuantity();
        }).sum();
        double pnl = totalValue - totalInvested;
        double pnlPct = totalInvested > 0 ? (pnl / totalInvested) * 100 : 0.0;

        List<String> symbolsList = userHoldings.stream().map(Holding::getSymbol).toList();
        String holdingsStr = String.join(", ", symbolsList);

        // Classify gainers and down positions
        List<Holding> gainers = new ArrayList<>();
        List<Holding> losers = new ArrayList<>();
        for (Holding h : userHoldings) {
            Double livePrice = liveQuotes.get(h.getSymbol());
            double cur = (livePrice != null && livePrice > 0) ? livePrice : (h.getCurrentPrice() > 0 ? h.getCurrentPrice() : h.getAvgPrice());
            if (cur >= h.getAvgPrice()) {
                gainers.add(h);
            } else {
                losers.add(h);
            }
        }

        String insightText = buildEducationalInsight(langCode, userHoldings.size(), holdingsStr, Math.round(totalValue), Math.round(pnl), pnlPct, trend, gainers, losers);

        return new InsightResult(insightText, langCode, "smart-rules", "SUCCESS");
    }

    private String buildEducationalInsight(String lang, int count, String symbols, long val, long pnl, double pnlPct, String trend, List<Holding> gainers, List<Holding> losers) {
        String pnlStr = (pnl >= 0 ? "+" : "") + "₹" + pnl + " (" + String.format(Locale.US, "%.1f", pnlPct) + "%)";
        boolean isPositive = "positive".equalsIgnoreCase(trend);

        String topGainerSym = !gainers.isEmpty() ? gainers.get(0).getSymbol() : "None";
        String topLoserSym = !losers.isEmpty() ? losers.get(0).getSymbol() : "None";

        switch (lang) {
            case "hi": {
                String trendWord = isPositive ? "सकारात्मक (Positive)" : "नकारात्मक (Negative)";
                StringBuilder sb = new StringBuilder();
                sb.append("आपके पोर्टफोलियो में ").append(count).append(" शेयर (").append(symbols).append(") हैं, जिनका कुल मूल्य ₹").append(val).append(" (P&L: ").append(pnlStr).append(") है। ");
                if (!gainers.isEmpty()) sb.append("मुनाफे वाले शेयर: ").append(topGainerSym).append("। ");
                if (!losers.isEmpty()) sb.append("घाटे वाले शेयर: ").append(topLoserSym).append("। ");
                sb.append("NIFTY 50 बाजार रुझान वर्तमान में ").append(trendWord).append(" है। ");
                sb.append(count == 1 ? "सलाह: केवल 1 शेयर में निवेश करने से एकाग्रता जोखिम बढ़ता है। विविधता लाएं।" : "सलाह: बहु-शेयर पोर्टफोलियो बाजार के उतार-चढ़ाव में विविधता (Diversification) का लाभ देता है।");
                return sb.toString();
            }
            case "te": {
                String trendWord = isPositive ? "సానుకూలంగా (Positive)" : "ప్రతికూలంగా (Negative)";
                StringBuilder sb = new StringBuilder();
                sb.append("మీ పోర్ట్‌ఫోలియోలో ").append(count).append(" షేర్లు (").append(symbols).append(") ఉన్నాయి, వీటి మొత్తం విలువ ₹").append(val).append(" (P&L: ").append(pnlStr).append("). ");
                if (!gainers.isEmpty()) sb.append("లాభాల్లో ఉన్న షేర్లు: ").append(topGainerSym).append(". ");
                if (!losers.isEmpty()) sb.append("నష్టాల్లో ఉన్న షేర్లు: ").append(topLoserSym).append(". ");
                sb.append("NIFTY 50 మార్కెట్ పోకడ ప్రస్తుతం ").append(trendWord).append(" ఉంది. ");
                sb.append(count == 1 ? "సూచన: ఒకే షేరులో పెట్టుబడి పెట్టడం వల్ల రిస్క్ పెరుగుతుంది. విభిన్న రంగాలలో విస్తరించండి." : "సూచన: విభిన్న షేర్ల పోర్ట్‌ఫోలియో మార్కెట్ హెచ్చుతగ్గుల నుండి నష్టభయాన్ని తగ్గిస్తుంది.");
                return sb.toString();
            }
            case "ta": {
                String trendWord = isPositive ? "நேர்மறையாக (Positive)" : "எதிர்மறையாக (Negative)";
                StringBuilder sb = new StringBuilder();
                sb.append("உங்கள் போர்ட்ஃபோலியோவில் ").append(count).append(" பங்குகள் (").append(symbols).append(") உள்ளன, மொத்த மதிப்பு ₹").append(val).append(" (P&L: ").append(pnlStr).append("). ");
                if (!gainers.isEmpty()) sb.append("லாபகரமான பங்குகள்: ").append(topGainerSym).append(". ");
                if (!losers.isEmpty()) sb.append("நஷ்டத்தில் உள்ள பங்குகள்: ").append(topLoserSym).append(". ");
                sb.append("NIFTY 50 சந்தைப் போக்கு தற்போது ").append(trendWord).append(" உள்ளது. ");
                sb.append("அறிவுரை: பல துறைகளில் முதலீட்டைப் விரிவுபடுத்துவது (Diversification) சந்தைப் அபாயத்தைக் குறைக்கும்.");
                return sb.toString();
            }
            case "bn": {
                String trendWord = isPositive ? "ইতিবাচক (Positive)" : "নেতিবাচক (Negative)";
                StringBuilder sb = new StringBuilder();
                sb.append("আপনার পোর্টফোলিওতে ").append(count).append("টি শেয়ার (").append(symbols).append(") রয়েছে যার মোট মূল্য ₹").append(val).append(" (P&L: ").append(pnlStr).append(")। ");
                if (!gainers.isEmpty()) sb.append("লাভজনক শেয়ার: ").append(topGainerSym).append("। ");
                if (!losers.isEmpty()) sb.append("লোকসানজনক শেয়ার: ").append(topLoserSym).append("। ");
                sb.append("NIFTY 50 বাজারের প্রবণতা বর্তমানে ").append(trendWord).append("। ");
                sb.append("পরামর্শ: বিভিন্ন খাতে বিনিয়োগ বৈচিত্র্যকরণ (Diversification) বাজারের ঝুঁকি কমায়।");
                return sb.toString();
            }
            case "gu": {
                String trendWord = isPositive ? "હકારાત્મક (Positive)" : "નકારાત્મક (Negative)";
                StringBuilder sb = new StringBuilder();
                sb.append("તમારા પોર્ટફોલિયોમાં ").append(count).append(" શેર (").append(symbols).append(") છે જેનું કુલ મૂલ્ય ₹").append(val).append(" (P&L: ").append(pnlStr).append(") છે. ");
                if (!gainers.isEmpty()) sb.append("નફાકારક શેર: ").append(topGainerSym).append(". ");
                if (!losers.isEmpty()) sb.append("નુકસાનમાં શેર: ").append(topLoserSym).append(". ");
                sb.append("NIFTY 50 બજારનું વલણ હાલમાં ").append(trendWord).append(" છે. ");
                sb.append("સલાહ: વિવિધ ક્ષેત્રોમાં રોકાણનું વૈવિધ્યકરણ (Diversification) બજારના જોખમને ઘટાડે છે.");
                return sb.toString();
            }
            case "mr": {
                String trendWord = isPositive ? "सकारात्मक (Positive)" : "नकारात्मक (Negative)";
                StringBuilder sb = new StringBuilder();
                sb.append("तुमच्या पोर्टफोलिओमध्ये ").append(count).append(" शेअर्स (").append(symbols).append(") आहेत ज्यांचे एकूण मूल्य ₹").append(val).append(" (P&L: ").append(pnlStr).append(") आहे. ");
                if (!gainers.isEmpty()) sb.append("फायद्यातील शेअर्स: ").append(topGainerSym).append(". ");
                if (!losers.isEmpty()) sb.append("तोट्यातील शेअर्स: ").append(topLoserSym).append(". ");
                sb.append("NIFTY 50 बाजाराचा कल सध्या ").append(trendWord).append(" आहे. ");
                sb.append("सल्ला: विविध क्षेत्रांमध्ये गुंतवणुकीचे विविधीकरण (Diversification) बाजारातील धोका कमी करते.");
                return sb.toString();
            }
            case "kn": {
                String trendWord = isPositive ? "ಸಕಾರಾತ್ಮಕ (Positive)" : "ನಕಾರಾತ್ಮಕ (Negative)";
                StringBuilder sb = new StringBuilder();
                sb.append("ನಿಮ್ಮ ಪೋರ್ಟ್‌ಫೋಲಿಯೊದಲ್ಲಿ ₹").append(val).append(" ಮೌಲ್ಯದ ").append(count).append(" ಷೇರುಗಳು (").append(symbols).append(") ಇವೆ (P&L: ").append(pnlStr).append("). ");
                if (!gainers.isEmpty()) sb.append("ಲಾಭದಲ್ಲಿರುವ ಷೇರುಗಳು: ").append(topGainerSym).append(". ");
                if (!losers.isEmpty()) sb.append("ನಷ್ಟದಲ್ಲಿರುವ ಷೇರುಗಳು: ").append(topLoserSym).append(". ");
                sb.append("NIFTY 50 ಮಾರುಕಟ್ಟೆ ಪ್ರವೃತ್ತಿ ಪ್ರಸ್ತುತ ").append(trendWord).append(" ಆಗಿದೆ. ");
                sb.append("ಸಲಹೆ: ವೈವಿಧ್ಯಮಯ ಷೇರುಗಳಲ್ಲಿ ಹೂಡಿಕೆ ಮಾಡುವುದು (Diversification) ಅಪಾಯವನ್ನು ಕಡಿಮೆ ಮಾಡುತ್ತದೆ.");
                return sb.toString();
            }
            case "ml": {
                String trendWord = isPositive ? "അനുകൂലം (Positive)" : "പ്രതികൂലം (Negative)";
                StringBuilder sb = new StringBuilder();
                sb.append("നിങ്ങളുടെ പോർട്ട്ഫോളിയോയിൽ ₹").append(val).append(" മൂല്യമുള്ള ").append(count).append(" ഓഹരികൾ (").append(symbols).append(") ഉണ്ട് (P&L: ").append(pnlStr).append("). ");
                if (!gainers.isEmpty()) sb.append("ലാഭത്തിലുള്ള ഓഹരികൾ: ").append(topGainerSym).append(". ");
                if (!losers.isEmpty()) sb.append("നഷ്ടത്തിലുള്ള ഓഹരികൾ: ").append(topLoserSym).append(". ");
                sb.append("NIFTY 50 വിപണി പ്രവണത നിലവിൽ ").append(trendWord).append(" ആണ്. ");
                sb.append("ഉപദേശം: വൈവിധ്യമാർന്ന നിക്ഷേപം (Diversification) വിപണിയിലെ നഷ്ടസാധ്യത കുറയ്ക്കുന്നു.");
                return sb.toString();
            }
            default: {
                String trendWord = isPositive ? "positive" : "negative";
                StringBuilder sb = new StringBuilder();
                sb.append("Your portfolio holds ").append(count).append(" positions (").append(symbols).append(") with a total value of ₹").append(val).append(" (P&L: ").append(pnlStr).append("). ");
                if (!gainers.isEmpty()) sb.append("Top gainers include ").append(topGainerSym).append(". ");
                if (!losers.isEmpty()) sb.append("Positions below cost: ").append(topLoserSym).append(". ");
                sb.append("NIFTY 50 market trend is currently ").append(trendWord).append(". ");
                sb.append(count == 1 ? "Financial Tip: Single-asset holdings carry high concentration risk. Consider diversifying across multiple market sectors to reduce risk exposure." : "Financial Tip: Holding a multi-stock portfolio provides diversification benefits against short-term market volatility. Continue disciplined asset allocation.");
                return sb.toString();
            }
        }
    }

    private String resolveLanguage(UUID userId, String language) {
        if (language != null && !language.isBlank()) {
            return normalizeLanguage(language);
        }
        if (userId != null) {
            try {
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("X-User-Id", userId.toString());
                org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
                org.springframework.http.ResponseEntity<Map> resp = restTemplate.exchange(
                        "http://localhost:8082/profile/me",
                        org.springframework.http.HttpMethod.GET,
                        entity,
                        Map.class
                );
                if (resp.getBody() != null && resp.getBody().get("language") != null) {
                    String profLang = String.valueOf(resp.getBody().get("language"));
                    if (!profLang.isBlank() && !"null".equalsIgnoreCase(profLang)) {
                        return normalizeLanguage(profLang);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch profile language setting: " + e.getMessage());
            }
        }
        return "en";
    }

    private String normalizeLanguage(String lang) {
        if (lang == null || lang.isBlank()) return "en";
        String l = lang.trim().toLowerCase();
        if (l.contains("hindi") || l.equals("hi")) return "hi";
        if (l.contains("telugu") || l.equals("te")) return "te";
        if (l.contains("tamil") || l.equals("ta")) return "ta";
        if (l.contains("bengali") || l.equals("bn")) return "bn";
        if (l.contains("gujarati") || l.equals("gu")) return "gu";
        if (l.contains("marathi") || l.equals("mr")) return "mr";
        if (l.contains("kannada") || l.equals("kn")) return "kn";
        if (l.contains("malayalam") || l.equals("ml")) return "ml";
        return "en";
    }

    private Map<String, Double> fetchLiveQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) return Map.of();
        try {
            List<Map<String, Object>> quotes = restTemplate.postForObject("http://localhost:8087/market/quotes", symbols, List.class);
            if (quotes != null) {
                Map<String, Double> prices = new HashMap<>();
                for (Map<String, Object> q : quotes) {
                    String rawSym = (String) q.get("symbol");
                    String cleanSym = rawSym != null ? rawSym.replace("NSE:", "").trim() : "";
                    double price = ((Number) q.getOrDefault("price", 0.0)).doubleValue();
                    if (!cleanSym.isEmpty() && price > 0) {
                        prices.put(cleanSym, price);
                    }
                }
                return prices;
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch live quotes for AI request: " + e.getMessage());
        }
        return Map.of();
    }

    private String fetchMarketTrend() {
        try {
            List<Map<String, Object>> indices = restTemplate.getForObject(MARKET_DATA_URL, List.class);
            if (indices != null && !indices.isEmpty()) {
                Map<String, Object> nifty = indices.get(0);
                double changePct = ((Number) nifty.getOrDefault("changePct", 0.0)).doubleValue();
                return changePct >= 0 ? "positive" : "negative";
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch market indices for AI trend: " + e.getMessage());
        }
        return "positive";
    }
}
