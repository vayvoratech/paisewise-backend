package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.domain.Holding;
import in.sapphirus.rupee.portfolio.repo.HoldingRepository;
import in.sapphirus.rupee.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import in.sapphirus.rupee.portfolio.service.AiPortfolioInsightService;
import in.sapphirus.rupee.portfolio.service.AiPortfolioInsightService.InsightResult;

/** Holdings + plain-English P&L summary. Routed at /portfolio/**. */
@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    private final HoldingRepository holdings;
    private final RestTemplate restTemplate;
    private final AiPortfolioInsightService aiInsightService;

    public PortfolioController(HoldingRepository holdings, AiPortfolioInsightService aiInsightService) {
        this.holdings = holdings;
        this.aiInsightService = aiInsightService;
        this.restTemplate = new RestTemplate();
    }

    public record AiInsightRequest(String language) {}

    public record HoldingView(
            String symbol,
            String name,
            String emoji,
            int quantity,
            int shares,
            double avgCost,
            double avgPrice,
            double currentPrice,
            double value,
            double gainAbs,
            double gainPct,
            String note
    ) {}

    public record SummaryView(double holdingsValue, double invested, double gainAbs, double gainPct,
                              String insight, List<HoldingView> holdings) {}

    public record BuyHoldingRequest(String symbol, String name, String emoji, int shares, double price) {}

    @GetMapping("/me")
    public SummaryView mySummary() {
        UUID userUuid = UUID.fromString(CurrentUser.requireId());
        List<Holding> mine = holdings.findByUserId(userUuid);
        List<String> symbols = mine.stream().map(Holding::getSymbol).toList();
        Map<String, Double> livePrices = fetchLiveQuotes(symbols);

        List<HoldingView> views = mine.stream().map(h -> viewWithLivePrice(h, livePrices.get(h.getSymbol()))).toList();
        double value = views.stream().mapToDouble(HoldingView::value).sum();
        double invested = mine.stream().mapToDouble(h -> h.getAvgCost().doubleValue() * h.getQuantity()).sum();
        double gainAbs = value - invested;
        double gainPct = invested == 0 ? 0 : (gainAbs / invested) * 100;
        return new SummaryView(round(value, 2), round(invested, 2), round(gainAbs, 2), round(gainPct, 1),
                "Live market prices updated.", views);
    }

    @GetMapping("/me/holdings")
    public List<HoldingView> myHoldings() {
        UUID userUuid = UUID.fromString(CurrentUser.requireId());
        List<Holding> mine = holdings.findByUserId(userUuid);
        List<String> symbols = mine.stream().map(Holding::getSymbol).toList();
        Map<String, Double> livePrices = fetchLiveQuotes(symbols);
        return mine.stream().map(h -> viewWithLivePrice(h, livePrices.get(h.getSymbol()))).toList();
    }

    @PostMapping("/ai-insight")
    public InsightResult generateAiInsightPost(@RequestBody(required = false) AiInsightRequest req) {
        UUID userUuid = UUID.fromString(CurrentUser.requireId());
        String lang = (req != null && req.language() != null) ? req.language() : "en";
        return aiInsightService.generateInsight(userUuid, lang);
    }

    @GetMapping("/ai-insight")
    public InsightResult generateAiInsightGet(@RequestParam(name = "lang", defaultValue = "en") String lang) {
        UUID userUuid = UUID.fromString(CurrentUser.requireId());
        return aiInsightService.generateInsight(userUuid, lang);
    }

    @PostMapping("/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldingView buyHolding(@RequestBody BuyHoldingRequest req) {
        UUID userUuid = UUID.fromString(CurrentUser.requireId());
        String cleanSymbol = req.symbol() != null ? req.symbol().replace("NSE:", "").trim() : "UNKNOWN";
        int shares = req.shares() > 0 ? req.shares() : 1;
        double price = req.price() > 0 ? req.price() : 1.0;

        Optional<Holding> existingOpt = holdings.findByUserIdAndSymbol(userUuid, cleanSymbol);
        Holding holding;
        if (existingOpt.isPresent()) {
            holding = existingOpt.get();
            int totalShares = holding.getQuantity() + shares;
            double newAvgCost = ((holding.getAvgPrice() * holding.getQuantity()) + (price * shares)) / totalShares;
            holding.setQuantity(totalShares);
            holding.setAvgPrice(round(newAvgCost, 2));
            holding.setCurrentPrice(price);
            holding.setTotalInvested(round(totalShares * newAvgCost, 2));
            holding.setUpdatedAt(Instant.now());
        } else {
            holding = new Holding(userUuid, cleanSymbol, shares, price, price,
                    "You bought " + shares + " shares at ₹" + Math.round(price) + ". Position active.");
        }
        Holding saved = holdings.save(holding);
        return viewWithLivePrice(saved, price);
    }

    @GetMapping("/market-trend")
    public Map<String, Object> fetchMarketTrend() {
        try {
            List<Map<String, Object>> indices = restTemplate.getForObject("http://localhost:8087/market/indices", List.class);
            if (indices != null && !indices.isEmpty()) {
                Map<String, Object> nifty = indices.get(0);
                double changePct = ((Number) nifty.getOrDefault("changePct", 0.0)).doubleValue();
                String trend = changePct >= 0 ? "positive" : "negative";
                return Map.of("market", "NIFTY50", "trend", trend, "changePct", changePct, "status", "SUCCESS");
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch market trend from market-data-service: " + e.getMessage());
        }
        return Map.of("market", "NIFTY50", "trend", "positive", "status", "FALLBACK");
    }

    private HoldingView viewWithLivePrice(Holding h, Double livePrice) {
        double currentPrice = (livePrice != null && livePrice > 0) ? livePrice : (h.getCurrentPrice() > 0 ? h.getCurrentPrice() : h.getAvgPrice());
        double value = currentPrice * h.getQuantity();
        double avgCost = h.getAvgPrice();
        double gainAbs = (currentPrice - avgCost) * h.getQuantity();
        double gainPct = avgCost == 0 ? 0 : ((currentPrice - avgCost) / avgCost) * 100;
        String name = h.getName() != null && !h.getName().isBlank() ? h.getName() : h.getSymbol();
        String emoji = h.getEmoji() != null && !h.getEmoji().isBlank() ? h.getEmoji() : "📊";
        String note = h.getNote() != null && !h.getNote().isBlank() ? h.getNote() : "Position active.";

        return new HoldingView(
                h.getSymbol(),
                name,
                emoji,
                h.getQuantity(),
                h.getQuantity(),
                round(avgCost, 2),
                round(avgCost, 2),
                round(currentPrice, 2),
                round(value, 2),
                round(gainAbs, 2),
                round(gainPct, 1),
                note
        );
    }

    private Map<String, Double> fetchLiveQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) return Map.of();
        try {
            List<String> prefixed = symbols.stream().map(s -> s.contains(":") ? s : "NSE:" + s).toList();
            List<Map<String, Object>> quotes = restTemplate.postForObject("http://localhost:8087/market/quotes", prefixed, List.class);
            if (quotes != null) {
                Map<String, Double> prices = new HashMap<>();
                for (Map<String, Object> q : quotes) {
                    String rawSym = (String) q.get("symbol");
                    String cleanSym = rawSym != null ? rawSym.replace("NSE:", "").trim() : "";
                    double price = ((Number) q.getOrDefault("price", 0.0)).doubleValue();
                    if (price > 0) {
                        if (rawSym != null && !rawSym.isEmpty()) prices.put(rawSym, price);
                        if (!cleanSym.isEmpty()) prices.put(cleanSym, price);
                    }
                }
                return prices;
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch live quotes for portfolio summary: " + e.getMessage());
        }
        return Map.of();
    }

    private static double round(double v) { return round(v, 0); }
    private static double round(double v, int decimals) {
        double f = Math.pow(10, decimals);
        return Math.round(v * f) / f;
    }
}