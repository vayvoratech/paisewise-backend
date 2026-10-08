package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.domain.MfInvestment;
import in.sapphirus.rupee.portfolio.domain.MfScheme;
import in.sapphirus.rupee.portfolio.service.AiFundRecommendationService;
import in.sapphirus.rupee.portfolio.service.AmfiNavSyncService;
import in.sapphirus.rupee.portfolio.service.BseMfOrderService;
import in.sapphirus.rupee.portfolio.service.FundService;
import in.sapphirus.rupee.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Comprehensive Mutual Funds REST Controller for Scheme Search, AI Fund Recommendations,
 * Daily AMFI NAV Sync management, and BSE StarMF Order placement routes.
 */
@RestController
@RequestMapping({"/portfolio/funds", "/funds", "/portfolio/mf"})
@Validated
public class FundController {

    private final FundService fundService;
    private final BseMfOrderService bseOrderService;
    private final AmfiNavSyncService amfiSyncService;

    public FundController(FundService fundService,
                          BseMfOrderService bseOrderService,
                          AmfiNavSyncService amfiSyncService) {
        this.fundService = fundService;
        this.bseOrderService = bseOrderService;
        this.amfiSyncService = amfiSyncService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Catalog & Discovery Routes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Search and filter mutual fund schemes.
     */
    @GetMapping("/search")
    public Page<FundService.SchemeSummaryView> searchSchemes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String amc,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) Boolean isTaxSaver,
            @RequestParam(required = false) Double minSipMax,
            @RequestParam(required = false, defaultValue = "RETURNS_3Y") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        FundService.SchemeFilter filter = new FundService.SchemeFilter(
                query, category, amc, riskLevel, isTaxSaver, minSipMax, sortBy, page, size
        );
        return fundService.searchSchemes(filter);
    }

    /**
     * List all available mutual fund categories.
     */
    @GetMapping("/categories")
    public List<String> getCategories() {
        return fundService.getCategories();
    }

    /**
     * List all available AMCs (Asset Management Companies).
     */
    @GetMapping("/amcs")
    public List<String> getAmcs() {
        return fundService.getAmcs();
    }

    /**
     * Get top trending / top 3-year CAGR performers.
     */
    @GetMapping("/trending")
    public List<FundService.SchemeSummaryView> getTrending(
            @RequestParam(required = false, defaultValue = "6") int limit
    ) {
        return fundService.getTrendingFunds(limit);
    }

    /**
     * Get mutual fund scheme details by Scheme Code or ISIN.
     */
    @GetMapping("/{schemeCode}")
    public ResponseEntity<MfScheme> getSchemeDetails(@PathVariable String schemeCode) {
        return fundService.getScheme(schemeCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI Recommendation Routes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * AI-driven Mutual Fund recommendations based on risk appetite, goal, and horizon.
     */
    @PostMapping("/recommendations")
    public AiFundRecommendationService.RecommendationResponse getRecommendations(
            @RequestBody @Valid AiFundRecommendationService.RecommendationRequest request
    ) {
        return fundService.getAiRecommendations(request);
    }

    @GetMapping("/recommendations")
    public AiFundRecommendationService.RecommendationResponse getRecommendationsQuery(
            @RequestParam(required = false, defaultValue = "MODERATE") String riskAppetite,
            @RequestParam(required = false, defaultValue = "WEALTH_CREATION") String goal,
            @RequestParam(required = false, defaultValue = "3") Integer horizonYears,
            @RequestParam(required = false) Double monthlyBudget,
            @RequestParam(required = false) String categoryPreference,
            @RequestParam(required = false, defaultValue = "en") String language
    ) {
        AiFundRecommendationService.RecommendationRequest req = new AiFundRecommendationService.RecommendationRequest(
                riskAppetite, goal, horizonYears, monthlyBudget, categoryPreference, language
        );
        return fundService.getAiRecommendations(req);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BSE StarMF Order Routes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Place a Lumpsum Mutual Fund Purchase order on BSE StarMF.
     */
    @PostMapping("/orders/purchase")
    public ResponseEntity<BseMfOrderService.MfOrderReceipt> placeLumpsumPurchase(
            @RequestBody @Valid BseMfOrderService.LumpsumOrderRequest req
    ) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        BseMfOrderService.MfOrderReceipt receipt = bseOrderService.placeLumpsumOrder(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

    /**
     * Place a Mutual Fund Redemption order on BSE StarMF.
     */
    @PostMapping("/orders/redeem")
    public ResponseEntity<BseMfOrderService.MfOrderReceipt> placeRedemption(
            @RequestBody @Valid BseMfOrderService.RedemptionOrderRequest req
    ) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        BseMfOrderService.MfOrderReceipt receipt = bseOrderService.placeRedemptionOrder(userId, req);
        return ResponseEntity.ok(receipt);
    }

    /**
     * Place a Mutual Fund Switch order on BSE StarMF.
     */
    @PostMapping("/orders/switch")
    public ResponseEntity<BseMfOrderService.MfOrderReceipt> placeSwitch(
            @RequestBody @Valid BseMfOrderService.SwitchOrderRequest req
    ) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        BseMfOrderService.MfOrderReceipt receipt = bseOrderService.placeSwitchOrder(userId, req);
        return ResponseEntity.ok(receipt);
    }

    /**
     * Check the status of a BSE Mutual Fund order.
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<BseMfOrderService.MfOrderReceipt> checkOrderStatus(
            @PathVariable String orderId
    ) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return ResponseEntity.ok(bseOrderService.checkOrderStatus(orderId, userId));
    }

    /**
     * Get current user's Mutual Fund portfolio holdings & transaction history.
     */
    @GetMapping("/investments/me")
    public ResponseEntity<BseMfOrderService.UserMfPortfolioSummary> getMyMfPortfolio() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return ResponseEntity.ok(bseOrderService.getUserPortfolioSummary(userId));
    }

    /**
     * Get current user's recent Mutual Fund transactions list.
     */
    @GetMapping("/transactions/me")
    public ResponseEntity<List<MfInvestment>> getMyTransactions() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return ResponseEntity.ok(bseOrderService.getUserInvestments(userId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AMFI NAV Sync Administration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Manually trigger AMFI NAV sync job (Daily 7:00 PM sync trigger).
     */
    @PostMapping("/sync-nav")
    public ResponseEntity<AmfiNavSyncService.SyncResult> triggerNavSync() {
        AmfiNavSyncService.SyncResult result = amfiSyncService.syncNavNow();
        return ResponseEntity.ok(result);
    }
}
