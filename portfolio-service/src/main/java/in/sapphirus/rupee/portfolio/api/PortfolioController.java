package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.domain.Holding;
import in.sapphirus.rupee.portfolio.repo.HoldingRepository;
import in.sapphirus.rupee.security.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Holdings + plain-English P&L summary. Routed at /portfolio/**. */
@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    private final HoldingRepository holdings;

    public PortfolioController(HoldingRepository holdings) {
        this.holdings = holdings;
    }

    public record HoldingView(String symbol, int quantity, double avgCost, double currentPrice,
                              double value, double gainAbs, double gainPct) {}

    public record SummaryView(double holdingsValue, double invested, double gainAbs, double gainPct,
                              String insight, List<HoldingView> holdings) {}

    @GetMapping("/me")
    public SummaryView mySummary() {
        UUID userUuid = UUID.fromString(CurrentUser.requireId());
        List<Holding> mine = holdings.findByUserId(userUuid);
        List<HoldingView> views = mine.stream().map(this::view).toList();
        double value = views.stream().mapToDouble(HoldingView::value).sum();
        double invested = mine.stream().mapToDouble(h -> h.getAvgCost().doubleValue() * h.getQuantity()).sum();
        double gainAbs = value - invested;
        double gainPct = invested == 0 ? 0 : (gainAbs / invested) * 100;
        return new SummaryView(round(value), round(invested), round(gainAbs), round(gainPct, 1),
                "Live pricing coming soon — showing purchase value for now.", views);
    }

    @GetMapping("/me/holdings")
    public List<HoldingView> myHoldings() {
        UUID userUuid = UUID.fromString(CurrentUser.requireId());
        return holdings.findByUserId(userUuid).stream().map(this::view).toList();
    }

    private HoldingView view(Holding h) {
        double currentPrice = h.getAvgCost().doubleValue();
        double value = currentPrice * h.getQuantity();
        double avgCost = h.getAvgCost().doubleValue();
        double gainAbs = (currentPrice - avgCost) * h.getQuantity();
        double gainPct = avgCost == 0 ? 0 : ((currentPrice - avgCost) / avgCost) * 100;
        return new HoldingView(h.getSymbol(), h.getQuantity(), round(avgCost), round(currentPrice),
                round(value), round(gainAbs), round(gainPct, 1));
    }

    private static double round(double v) { return round(v, 0); }
    private static double round(double v, int decimals) {
        double f = Math.pow(10, decimals);
        return Math.round(v * f) / f;
    }
}