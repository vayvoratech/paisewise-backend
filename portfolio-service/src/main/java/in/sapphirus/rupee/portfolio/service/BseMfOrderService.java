package in.sapphirus.rupee.portfolio.service;

import in.sapphirus.rupee.portfolio.client.BseStarMfClient;
import in.sapphirus.rupee.portfolio.domain.MfInvestment;
import in.sapphirus.rupee.portfolio.domain.MfScheme;
import in.sapphirus.rupee.portfolio.repo.MfInvestmentRepository;
import in.sapphirus.rupee.portfolio.repo.MfSchemeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing BSE StarMF order execution workflows, redemptions, switches, and portfolio holding calculations.
 */
@Service
public class BseMfOrderService {

    private static final Logger log = LoggerFactory.getLogger(BseMfOrderService.class);

    private final MfInvestmentRepository investmentRepo;
    private final MfSchemeRepository schemeRepo;
    private final BseStarMfClient bseClient;

    public BseMfOrderService(MfInvestmentRepository investmentRepo,
                             MfSchemeRepository schemeRepo,
                             BseStarMfClient bseClient) {
        this.investmentRepo = investmentRepo;
        this.schemeRepo = schemeRepo;
        this.bseClient = bseClient;
    }

    public record LumpsumOrderRequest(
            String schemeCode,
            double amount,
            String folioNumber
    ) {}

    public record RedemptionOrderRequest(
            String schemeCode,
            String folioNumber,
            Double amount,
            Double units,
            boolean allUnits
    ) {}

    public record SwitchOrderRequest(
            String fromSchemeCode,
            String toSchemeCode,
            String folioNumber,
            Double amount,
            Double units
    ) {}

    public record MfOrderReceipt(
            UUID investmentId,
            UUID userId,
            String schemeCode,
            String schemeName,
            String transactionType,
            String status,
            double amount,
            Double unitsAllotted,
            Double navApplied,
            String folioNumber,
            String bseOrderId,
            String message,
            Instant transactionDate
    ) {
        public static MfOrderReceipt from(MfInvestment inv, String schemeName, String message) {
            return new MfOrderReceipt(
                    inv.getId(),
                    inv.getUserId(),
                    inv.getSchemeCode(),
                    schemeName,
                    inv.getTransactionType(),
                    inv.getStatus(),
                    inv.getAmount(),
                    inv.getUnitsAllotted(),
                    inv.getNavApplied(),
                    inv.getFolioNumber(),
                    inv.getBseOrderId(),
                    message,
                    inv.getTransactionDate()
            );
        }
    }

    public record UserMfHoldingItem(
            String schemeCode,
            String schemeName,
            String amcName,
            String category,
            double totalUnits,
            double investedAmount,
            double currentNav,
            double currentValue,
            double absoluteGain,
            double percentageGain,
            String folioNumber
    ) {}

    public record UserMfPortfolioSummary(
            double totalInvested,
            double totalCurrentValue,
            double totalGainAbs,
            double totalGainPct,
            int totalSchemesCount,
            List<UserMfHoldingItem> holdings,
            List<MfInvestment> recentTransactions
    ) {}

    /**
     * Place a Lumpsum Purchase order with BSE StarMF.
     */
    @Transactional
    public MfOrderReceipt placeLumpsumOrder(UUID userId, LumpsumOrderRequest req) {
        MfScheme scheme = schemeRepo.findById(req.schemeCode())
                .orElseThrow(() -> new IllegalArgumentException("Mutual fund scheme not found: " + req.schemeCode()));

        if (req.amount() < scheme.getMinLumpsum()) {
            throw new IllegalArgumentException(String.format("Minimum lumpsum purchase for %s is ₹%.0f",
                    scheme.getSchemeName(), scheme.getMinLumpsum()));
        }

        MfInvestment investment = new MfInvestment(userId, scheme.getSchemeCode(), req.amount());
        investment.setTransactionType("PURCHASE");
        investment.setStatus("PENDING");
        investment.setFolioNumber(req.folioNumber() != null ? req.folioNumber() : "FOLIO_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        investment.setNavApplied(scheme.getNav());
        double units = scheme.getNav() > 0 ? (req.amount() / scheme.getNav()) : 0.0;
        investment.setUnitsAllotted(Math.round(units * 10000.0) / 10000.0);

        investment = investmentRepo.save(investment);

        try {
            String bseOrderId = bseClient.submitPurchase(userId, scheme.getSchemeCode(), req.amount(), investment.getFolioNumber(), investment.getId());
            investment.setBseOrderId(bseOrderId);
            investment.setStatus("SUBMITTED");
            investment.setBseRemarks("Order accepted by BSE StarMF");
            investmentRepo.save(investment);

            log.info("Lumpsum purchase order submitted successfully: userId={}, orderId={}, scheme={}", userId, bseOrderId, scheme.getSchemeCode());
            return MfOrderReceipt.from(investment, scheme.getSchemeName(), "Purchase order placed successfully on BSE StarMF");
        } catch (Exception e) {
            log.error("Failed placing BSE lumpsum order: {}", e.getMessage(), e);
            investment.setStatus("FAILED");
            investment.setBseRemarks("BSE StarMF Error: " + e.getMessage());
            investmentRepo.save(investment);
            return MfOrderReceipt.from(investment, scheme.getSchemeName(), "Order failed: " + e.getMessage());
        }
    }

    /**
     * Place a Redemption order with BSE StarMF.
     */
    @Transactional
    public MfOrderReceipt placeRedemptionOrder(UUID userId, RedemptionOrderRequest req) {
        MfScheme scheme = schemeRepo.findById(req.schemeCode())
                .orElseThrow(() -> new IllegalArgumentException("Mutual fund scheme not found: " + req.schemeCode()));

        double redAmount = req.amount() != null ? req.amount() : 0.0;
        double redUnits = req.units() != null ? req.units() : 0.0;

        if (redAmount <= 0 && redUnits <= 0 && !req.allUnits()) {
            throw new IllegalArgumentException("Please specify amount or units to redeem.");
        }

        MfInvestment investment = new MfInvestment(userId, scheme.getSchemeCode(), redAmount);
        investment.setTransactionType("REDEMPTION");
        investment.setStatus("PENDING");
        investment.setFolioNumber(req.folioNumber());
        investment.setNavApplied(scheme.getNav());
        investment.setUnitsAllotted(redUnits > 0 ? redUnits : (scheme.getNav() > 0 ? redAmount / scheme.getNav() : 0.0));

        investment = investmentRepo.save(investment);

        try {
            String bseOrderId = bseClient.submitRedemption(userId, scheme.getSchemeCode(), req.folioNumber(), req.amount(), req.units(), req.allUnits());
            investment.setBseOrderId(bseOrderId);
            investment.setStatus("SUBMITTED");
            investment.setBseRemarks("Redemption request submitted to BSE StarMF");
            investmentRepo.save(investment);

            return MfOrderReceipt.from(investment, scheme.getSchemeName(), "Redemption request submitted to BSE StarMF. Funds will settle within T+2 business days.");
        } catch (Exception e) {
            log.error("Failed placing BSE redemption order: {}", e.getMessage(), e);
            investment.setStatus("FAILED");
            investment.setBseRemarks("BSE Redemption Error: " + e.getMessage());
            investmentRepo.save(investment);
            return MfOrderReceipt.from(investment, scheme.getSchemeName(), "Redemption failed: " + e.getMessage());
        }
    }

    /**
     * Place a Scheme Switch order with BSE StarMF.
     */
    @Transactional
    public MfOrderReceipt placeSwitchOrder(UUID userId, SwitchOrderRequest req) {
        MfScheme fromScheme = schemeRepo.findById(req.fromSchemeCode())
                .orElseThrow(() -> new IllegalArgumentException("Source scheme not found: " + req.fromSchemeCode()));
        MfScheme toScheme = schemeRepo.findById(req.toSchemeCode())
                .orElseThrow(() -> new IllegalArgumentException("Target scheme not found: " + req.toSchemeCode()));

        double switchAmount = req.amount() != null ? req.amount() : 0.0;

        MfInvestment investment = new MfInvestment(userId, req.fromSchemeCode(), switchAmount);
        investment.setTransactionType("SWITCH_OUT");
        investment.setStatus("PENDING");
        investment.setFolioNumber(req.folioNumber());
        investment.setNavApplied(fromScheme.getNav());

        investment = investmentRepo.save(investment);

        try {
            String bseOrderId = bseClient.submitSwitch(userId, req.fromSchemeCode(), req.toSchemeCode(), req.folioNumber(), req.amount(), req.units());
            investment.setBseOrderId(bseOrderId);
            investment.setStatus("SUBMITTED");
            investment.setBseRemarks("Switch order submitted to " + toScheme.getSchemeName());
            investmentRepo.save(investment);

            return MfOrderReceipt.from(investment, fromScheme.getSchemeName(), "Switch order placed into " + toScheme.getSchemeName());
        } catch (Exception e) {
            log.error("Failed placing BSE switch order: {}", e.getMessage(), e);
            investment.setStatus("FAILED");
            investment.setBseRemarks("BSE Switch Error: " + e.getMessage());
            investmentRepo.save(investment);
            return MfOrderReceipt.from(investment, fromScheme.getSchemeName(), "Switch failed: " + e.getMessage());
        }
    }

    /**
     * Check order status by investment ID or BSE Order ID.
     */
    public MfOrderReceipt checkOrderStatus(String orderIdOrInvestId, UUID userId) {
        Optional<MfInvestment> optInv;
        try {
            UUID invId = UUID.fromString(orderIdOrInvestId);
            optInv = investmentRepo.findById(invId);
        } catch (Exception e) {
            optInv = investmentRepo.findByBseOrderId(orderIdOrInvestId);
        }

        MfInvestment inv = optInv.orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderIdOrInvestId));
        if (!inv.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to order " + orderIdOrInvestId);
        }

        MfScheme scheme = schemeRepo.findById(inv.getSchemeCode()).orElse(null);
        String schemeName = scheme != null ? scheme.getSchemeName() : inv.getSchemeCode();

        if (inv.getBseOrderId() != null) {
            var bseStatus = bseClient.queryOrderStatus(inv.getBseOrderId());
            if ("ALLOTTED".equalsIgnoreCase(bseStatus.status()) && !"ALLOTTED".equalsIgnoreCase(inv.getStatus())) {
                inv.setStatus("ALLOTTED");
                inv.setBseRemarks(bseStatus.remarks());
                investmentRepo.save(inv);
            }
        }

        return MfOrderReceipt.from(inv, schemeName, inv.getBseRemarks() != null ? inv.getBseRemarks() : "Status: " + inv.getStatus());
    }

    /**
     * Get all transactions for the current user.
     */
    public List<MfInvestment> getUserInvestments(UUID userId) {
        return investmentRepo.findByUserIdOrderByTransactionDateDesc(userId);
    }

    /**
     * Get aggregate Mutual Fund portfolio summary with holdings, current values, and total P&L.
     */
    public UserMfPortfolioSummary getUserPortfolioSummary(UUID userId) {
        List<MfInvestment> txns = investmentRepo.findByUserIdOrderByTransactionDateDesc(userId);

        Map<String, List<MfInvestment>> byScheme = txns.stream()
                .collect(Collectors.groupingBy(MfInvestment::getSchemeCode));

        List<UserMfHoldingItem> holdings = new ArrayList<>();
        double totalInvested = 0.0;
        double totalCurrentValue = 0.0;

        for (Map.Entry<String, List<MfInvestment>> entry : byScheme.entrySet()) {
            String code = entry.getKey();
            List<MfInvestment> schemeTxns = entry.getValue();
            MfScheme scheme = schemeRepo.findById(code).orElse(null);

            double unitsBought = schemeTxns.stream()
                    .filter(t -> "PURCHASE".equalsIgnoreCase(t.getTransactionType()) || "SIP".equalsIgnoreCase(t.getTransactionType()) || "SWITCH_IN".equalsIgnoreCase(t.getTransactionType()))
                    .filter(t -> !"FAILED".equalsIgnoreCase(t.getStatus()) && !"CANCELLED".equalsIgnoreCase(t.getStatus()))
                    .mapToDouble(t -> t.getUnitsAllotted() != null ? t.getUnitsAllotted() : (scheme != null && scheme.getNav() > 0 ? t.getAmount() / scheme.getNav() : 0))
                    .sum();

            double unitsRedeemed = schemeTxns.stream()
                    .filter(t -> "REDEMPTION".equalsIgnoreCase(t.getTransactionType()) || "SWITCH_OUT".equalsIgnoreCase(t.getTransactionType()))
                    .filter(t -> !"FAILED".equalsIgnoreCase(t.getStatus()) && !"CANCELLED".equalsIgnoreCase(t.getStatus()))
                    .mapToDouble(t -> t.getUnitsAllotted() != null ? t.getUnitsAllotted() : 0)
                    .sum();

            double netUnits = Math.max(0.0, unitsBought - unitsRedeemed);
            if (netUnits <= 0.0001) {
                continue;
            }

            double invested = schemeTxns.stream()
                    .filter(t -> "PURCHASE".equalsIgnoreCase(t.getTransactionType()) || "SIP".equalsIgnoreCase(t.getTransactionType()))
                    .filter(t -> !"FAILED".equalsIgnoreCase(t.getStatus()) && !"CANCELLED".equalsIgnoreCase(t.getStatus()))
                    .mapToDouble(MfInvestment::getAmount)
                    .sum();

            double currentNav = scheme != null ? scheme.getNav() : 100.0;
            double curVal = netUnits * currentNav;
            double gainAbs = curVal - invested;
            double gainPct = invested > 0 ? (gainAbs / invested) * 100.0 : 0.0;

            String folio = schemeTxns.stream()
                    .map(MfInvestment::getFolioNumber)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("FOLIO_" + code);

            holdings.add(new UserMfHoldingItem(
                    code,
                    scheme != null ? scheme.getSchemeName() : code,
                    scheme != null ? scheme.getAmcName() : "AMC",
                    scheme != null ? scheme.getCategory() : "Equity",
                    round(netUnits, 4),
                    round(invested, 2),
                    round(currentNav, 2),
                    round(curVal, 2),
                    round(gainAbs, 2),
                    round(gainPct, 2),
                    folio
            ));

            totalInvested += invested;
            totalCurrentValue += curVal;
        }

        double totalGainAbs = totalCurrentValue - totalInvested;
        double totalGainPct = totalInvested > 0 ? (totalGainAbs / totalInvested) * 100.0 : 0.0;

        return new UserMfPortfolioSummary(
                round(totalInvested, 2),
                round(totalCurrentValue, 2),
                round(totalGainAbs, 2),
                round(totalGainPct, 2),
                holdings.size(),
                holdings,
                txns
        );
    }

    private double round(double v, int decimals) {
        double f = Math.pow(10, decimals);
        return Math.round(v * f) / f;
    }
}
