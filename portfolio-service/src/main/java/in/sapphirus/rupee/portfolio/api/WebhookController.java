package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.client.FyersGateway;
import in.sapphirus.rupee.portfolio.domain.Holding;
import in.sapphirus.rupee.portfolio.domain.Ledger;
import in.sapphirus.rupee.portfolio.domain.Order;
import in.sapphirus.rupee.portfolio.domain.Trade;
import in.sapphirus.rupee.portfolio.event.PortfolioEventPublisher;
import in.sapphirus.rupee.portfolio.event.PortfolioRecalcEvent;
import in.sapphirus.rupee.portfolio.repo.HoldingRepository;
import in.sapphirus.rupee.portfolio.repo.LedgerRepository;
import in.sapphirus.rupee.portfolio.repo.OrderRepository;
import in.sapphirus.rupee.portfolio.repo.TradeRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Fyers Broker Webhook Handler Controller.
 *
 * Processes trade execution callbacks from the Fyers broker:
 * 1. Verifies HMAC-SHA256 signature on raw request body.
 * 2. Idempotency: ignores already processed brokerTradeIds.
 * 3. Inserts trade execution log into portfolio.trades.
 * 4. Credits/Debits cash ledger into portfolio.ledger.
 * 5. Updates user portfolio holdings.
 * 6. Updates order status and fills.
 * 7. Publishes portfolio.recalc event for async valuation recomputation.
 */
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final FyersGateway fyersGateway;
    private final TradeRepository tradeRepo;
    private final LedgerRepository ledgerRepo;
    private final HoldingRepository holdingRepo;
    private final OrderRepository orderRepo;
    private final PortfolioEventPublisher eventPublisher;

    public WebhookController(FyersGateway fyersGateway,
                             TradeRepository tradeRepo,
                             LedgerRepository ledgerRepo,
                             HoldingRepository holdingRepo,
                             OrderRepository orderRepo,
                             PortfolioEventPublisher eventPublisher) {
        this.fyersGateway = fyersGateway;
        this.tradeRepo = tradeRepo;
        this.ledgerRepo = ledgerRepo;
        this.holdingRepo = holdingRepo;
        this.orderRepo = orderRepo;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Webhook receiver for Fyers trade executions.
     *
     * @param rawBody raw request body for HMAC-SHA256 signature calculation
     * @param signature value of X-Fyers-Signature or X-Broker-Signature header
     */
    @PostMapping({"/fyers", "/trade"})
    @Transactional
    public ResponseEntity<String> handleFyersWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Fyers-Signature", required = false) String signature,
            @RequestHeader(value = "X-Broker-Signature", required = false) String altSignature) {

        String activeSignature = signature != null ? signature : altSignature;

        // ── 1. Verify HMAC-SHA256 Signature ──────────────────────────────────
        if (activeSignature == null || !fyersGateway.verifyWebhookSignature(rawBody, activeSignature)) {
            log.warn("Fyers webhook rejected: invalid or missing HMAC-SHA256 signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Invalid webhook signature\"}");
        }

        // ── 2. Parse JSON Payload ─────────────────────────────────────────────
        JSONObject payload;
        try {
            payload = new JSONObject(rawBody);
        } catch (Exception e) {
            log.error("Failed to parse Fyers webhook JSON: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Malformed JSON payload\"}");
        }

        // Support both direct fields and nested trade object
        JSONObject tradeData = payload.has("trade") ? payload.getJSONObject("trade") : payload;

        String brokerTradeId = tradeData.optString("brokerTradeId",
                tradeData.optString("tradeId", "TRD-" + System.currentTimeMillis()));
        String brokerOrderId = tradeData.optString("brokerOrderId",
                tradeData.optString("orderId", null));
        String clientOrderId = tradeData.optString("clientOrderId", null);

        // ── 3. Idempotency Check on Trade Execution ───────────────────────────
        if (tradeRepo.existsByBrokerTradeId(brokerTradeId)) {
            log.info("Duplicate trade callback skipped: brokerTradeId={}", brokerTradeId);
            return ResponseEntity.ok("{\"status\":\"already_processed\",\"brokerTradeId\":\"" + brokerTradeId + "\"}");
        }

        // ── 4. Resolve Order and User ─────────────────────────────────────────
        Optional<Order> orderOpt = Optional.empty();
        if (brokerOrderId != null) {
            orderOpt = orderRepo.findByBrokerOrderId(brokerOrderId);
        }
        if (orderOpt.isEmpty() && clientOrderId != null) {
            orderOpt = orderRepo.findByClientOrderId(clientOrderId);
        }

        UUID userId;
        UUID orderId = null;
        String symbol;
        String exchange = tradeData.optString("exchange", "NSE");
        String side;

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            userId = order.getUserId();
            orderId = order.getId();
            symbol = order.getSymbol();
            side = order.getSide();
        } else {
            // Direct callback payload with userId
            String userIdStr = tradeData.optString("userId", null);
            if (userIdStr == null || userIdStr.isBlank()) {
                log.error("Unable to resolve userId from order or webhook payload");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("{\"error\":\"Unresolved user for trade execution\"}");
            }
            userId = UUID.fromString(userIdStr);
            symbol = tradeData.optString("symbol", "UNKNOWN");
            side = tradeData.optString("side", "BUY").toUpperCase();
        }

        int fillQty = tradeData.optInt("fillQty", tradeData.optInt("quantity", 1));
        double fillPrice = tradeData.optDouble("fillPrice", tradeData.optDouble("price", 0.0));
        double brokerage = tradeData.optDouble("brokerage", 0.0);
        double stt = tradeData.optDouble("stt", 0.0);
        double gst = tradeData.optDouble("gst", 0.0);
        double sebiCharges = tradeData.optDouble("sebiCharges", 0.0);
        double stampDuty = tradeData.optDouble("stampDuty", 0.0);
        double totalCharges = tradeData.optDouble("totalCharges",
                brokerage + stt + gst + sebiCharges + stampDuty);

        double grossAmount = fillQty * fillPrice;
        double netAmount = "BUY".equalsIgnoreCase(side)
                ? grossAmount + totalCharges
                : grossAmount - totalCharges;

        // ── 5. Insert Trade Log ───────────────────────────────────────────────
        Trade trade = new Trade(orderId, userId, symbol, exchange, side, fillQty, fillPrice);
        trade.setBrokerage(brokerage);
        trade.setStt(stt);
        trade.setGst(gst);
        trade.setSebiCharges(sebiCharges);
        trade.setStampDuty(stampDuty);
        trade.setTotalCharges(totalCharges);
        trade.setNetAmount(netAmount);
        trade.setBrokerTradeId(brokerTradeId);
        trade.setPaper(false);
        trade.setTradedAt(Instant.now());

        try {
            trade = tradeRepo.save(trade);
            log.info("Persisted Trade log: tradeId={}, brokerTradeId={}, netAmount={}",
                    trade.getId(), brokerTradeId, netAmount);
        } catch (DataIntegrityViolationException e) {
            log.info("Concurrent trade duplicate detected for brokerTradeId={}", brokerTradeId);
            return ResponseEntity.ok("{\"status\":\"already_processed\"}");
        }

        // ── 6. Credit / Debit Cash Ledger ─────────────────────────────────────
        Double currentBalance = ledgerRepo.calculateBalance(userId);
        double balanceBefore = currentBalance != null ? currentBalance : 0.0;
        double balanceAfter;
        String ledgerType;
        String description;

        if ("BUY".equalsIgnoreCase(side)) {
            ledgerType = "DEBIT";
            balanceAfter = balanceBefore - netAmount;
            description = String.format("Trade DEBIT: Bought %d %s @ ₹%.2f (Charges: ₹%.2f)",
                    fillQty, symbol, fillPrice, totalCharges);
        } else {
            ledgerType = "CREDIT";
            balanceAfter = balanceBefore + netAmount;
            description = String.format("Trade CREDIT: Sold %d %s @ ₹%.2f (Charges: ₹%.2f)",
                    fillQty, symbol, fillPrice, totalCharges);
        }

        Ledger ledgerEntry = new Ledger(userId, ledgerType, netAmount, balanceAfter, description);
        ledgerEntry.setRefType("TRADE");
        ledgerEntry.setRefId(trade.getId());
        ledgerRepo.save(ledgerEntry);
        log.info("Updated Ledger entry: userId={}, type={}, amount={}, balanceAfter={}",
                userId, ledgerType, netAmount, balanceAfter);

        // ── 7. Update User Portfolio Holdings ─────────────────────────────────
        updateHoldings(userId, symbol, side, fillQty, fillPrice, netAmount);

        // ── 8. Update Order Status ────────────────────────────────────────────
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            int newFilledQty = order.getFilledQty() + fillQty;
            order.setFilledQty(newFilledQty);

            if (newFilledQty >= order.getQuantity()) {
                order.setStatus("COMPLETE");
            } else {
                order.setStatus("PARTIAL");
            }

            order.setAvgPrice(BigDecimal.valueOf(fillPrice).setScale(4, RoundingMode.HALF_UP));
            order.setUpdatedAt(Instant.now());
            orderRepo.save(order);
            log.info("Updated Order status: orderId={}, status={}, filledQty={}/{}",
                    order.getId(), order.getStatus(), order.getFilledQty(), order.getQuantity());
        }

        // ── 9. Publish portfolio.recalc Event ─────────────────────────────────
        PortfolioRecalcEvent recalcEvent = new PortfolioRecalcEvent(
                userId,
                symbol,
                trade.getId(),
                "TRADE_EXECUTION",
                Instant.now()
        );
        eventPublisher.publishPortfolioRecalc(recalcEvent);

        // ── 10. Return HTTP 200 OK ────────────────────────────────────────────
        return ResponseEntity.ok(String.format(
                "{\"status\":\"ok\",\"tradeId\":\"%s\",\"brokerTradeId\":\"%s\",\"message\":\"Trade processed successfully\"}",
                trade.getId(), brokerTradeId));
    }

    private void updateHoldings(UUID userId, String symbol, String side, int fillQty, double fillPrice, double netAmount) {
        Optional<Holding> holdingOpt = holdingRepo.findByUserIdAndSymbolAndProductAndIsPaper(userId, symbol, "CNC", false);

        if ("BUY".equalsIgnoreCase(side)) {
            if (holdingOpt.isPresent()) {
                Holding h = holdingOpt.get();
                int newQty = h.getQuantity() + fillQty;
                double newInvested = h.getTotalInvested() + netAmount;
                double newAvgCost = newQty > 0 ? (newInvested / newQty) : fillPrice;

                h.setQuantity(newQty);
                h.setTotalInvested(newInvested);
                h.setAvgCost(BigDecimal.valueOf(newAvgCost).setScale(4, RoundingMode.HALF_UP));
                h.setUpdatedAt(Instant.now());
                holdingRepo.save(h);
            } else {
                Holding newHolding = new Holding(userId, symbol, fillQty, fillPrice, netAmount, "CNC", false);
                holdingRepo.save(newHolding);
            }
        } else if ("SELL".equalsIgnoreCase(side) && holdingOpt.isPresent()) {
            Holding h = holdingOpt.get();
            int newQty = Math.max(0, h.getQuantity() - fillQty);
            double newInvested = newQty > 0 ? (h.getAvgCost().doubleValue() * newQty) : 0.0;

            h.setQuantity(newQty);
            h.setTotalInvested(newInvested);
            h.setUpdatedAt(Instant.now());
            holdingRepo.save(h);
        }
    }
}
