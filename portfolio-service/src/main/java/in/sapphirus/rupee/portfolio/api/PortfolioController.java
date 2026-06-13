package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.domain.Holding;
import in.sapphirus.rupee.portfolio.repo.HoldingRepository;
import in.sapphirus.rupee.security.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Holdings + plain-English P&L summary. Routed at /portfolio/**. */
@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    private static final double STARTING_CASH = 100_000;

    private final HoldingRepository holdings;

    public PortfolioController(HoldingRepository holdings) {
        this.holdings = holdings;
    }

    public record HoldingView(String symbol, String name, String emoji, int shares,
                              double avgPrice, double currentPrice, double value,
                              double gainAbs, double gainPct, String note) {}

    public record SummaryView(double holdingsValue, double invested, double gainAbs, double gainPct,
                              String insight, List<HoldingView> holdings) {}

    @GetMapping("/me")
    public SummaryView mySummary() {
        List<Holding> mine = holdings.findByUserId(CurrentUser.requireId());
        List<HoldingView> views = mine.stream().map(this::view).toList();
        double value = views.stream().mapToDouble(HoldingView::value).sum();
        double invested = mine.stream().mapToDouble(h -> h.getAvgPrice() * h.getShares()).sum();
        double gainAbs = value - invested;
        double gainPct = invested == 0 ? 0 : (gainAbs / invested) * 100;
        return new SummaryView(round(value), round(invested), round(gainAbs), round(gainPct, 1),
                "Reliance rose 1.2% — RBI kept interest rates unchanged. Good news for big companies!", views);
    }

    @GetMapping("/me/holdings")
    public List<HoldingView> myHoldings() {
        return holdings.findByUserId(CurrentUser.requireId()).stream().map(this::view).toList();
    }

    private HoldingView view(Holding h) {
        double value = h.getCurrentPrice() * h.getShares();
        double gainAbs = (h.getCurrentPrice() - h.getAvgPrice()) * h.getShares();
        double gainPct = h.getAvgPrice() == 0 ? 0 : ((h.getCurrentPrice() - h.getAvgPrice()) / h.getAvgPrice()) * 100;
        return new HoldingView(h.getSymbol(), h.getName(), h.getEmoji(), h.getShares(),
                h.getAvgPrice(), h.getCurrentPrice(), round(value), round(gainAbs), round(gainPct, 1), h.getNote());
    }

    private static double round(double v) { return round(v, 0); }
    private static double round(double v, int decimals) {
        double f = Math.pow(10, decimals);
        return Math.round(v * f) / f;
    }
}
