package in.sapphirus.rupee.portfolio.service;

import in.sapphirus.rupee.portfolio.domain.Holding;
import in.sapphirus.rupee.portfolio.exception.*;
import in.sapphirus.rupee.portfolio.repo.HoldingRepository;
import in.sapphirus.rupee.portfolio.repo.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Real-time Pre-trade Risk Management Engine.
 *
 * Enforces:
 * 1. KYC status == VERIFIED
 * 2. Order velocity limit (max 10 orders per minute)
 * 3. Open position limit (max 50 open positions)
 * 4. Margin requirement checks against user's ledger cash balance
 */
@Service
public class RiskEngine {

    private static final Logger log = LoggerFactory.getLogger(RiskEngine.class);

    public static final int MAX_OPEN_POSITIONS = 50;
    public static final int MAX_ORDERS_PER_MINUTE = 10;
    public static final String REQUIRED_KYC_STATUS = "VERIFIED";

    private final LedgerRepository ledgerRepo;
    private final HoldingRepository holdingRepo;

    // In-memory sliding-window tracker for order velocity (userId -> timestamps of orders in last 60s)
    private final Map<UUID, ConcurrentLinkedDeque<Instant>> userOrderTimestamps = new ConcurrentHashMap<>();

    public RiskEngine(LedgerRepository ledgerRepo, HoldingRepository holdingRepo) {
        this.ledgerRepo = ledgerRepo;
        this.holdingRepo = holdingRepo;
    }

    /**
     * Run all pre-trade risk checks for an incoming order.
     *
     * @param userId caller user ID
     * @param symbol trading symbol
     * @param side BUY or SELL
     * @param quantity order quantity
     * @param orderPrice order price (limit or estimated market price)
     * @param kycStatus verified KYC status
     */
    public void validateOrder(UUID userId, String symbol, String side, int quantity,
                              BigDecimal orderPrice, String kycStatus) {
        // 1. KYC verification check
        verifyKyc(userId, kycStatus);

        // 2. Order velocity check (10/min)
        checkOrderVelocity(userId);

        // 3. Open position limit check (50 positions)
        checkOpenPositionLimit(userId, symbol, side);

        // 4. Margin requirement check
        checkMargin(userId, side, quantity, orderPrice);

        // Record timestamp on passing all initial checks
        recordOrderAttempt(userId);
    }

    /**
     * 1. Verify user KYC is VERIFIED.
     */
    public void verifyKyc(UUID userId, String kycStatus) {
        if (kycStatus == null || !REQUIRED_KYC_STATUS.equalsIgnoreCase(kycStatus.trim())) {
            log.warn("Risk rejection for user {}: KYC is '{}', required '{}'", userId, kycStatus, REQUIRED_KYC_STATUS);
            throw new KycNotVerifiedException("User KYC must be VERIFIED before placing real market orders. Current status: "
                    + (kycStatus != null ? kycStatus : "UNVERIFIED"));
        }
    }

    /**
     * 2. Verify order velocity <= 10 orders per 60 seconds per user.
     */
    public void checkOrderVelocity(UUID userId) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(60, ChronoUnit.SECONDS);

        ConcurrentLinkedDeque<Instant> timestamps = userOrderTimestamps.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>());

        // Purge timestamps older than 60 seconds
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_ORDERS_PER_MINUTE) {
            log.warn("Risk rejection for user {}: order velocity limit exceeded ({} orders in last 60s)",
                    userId, timestamps.size());
            throw new OrderVelocityExceededException(
                    "Order velocity limit exceeded: Maximum " + MAX_ORDERS_PER_MINUTE + " orders per minute allowed.");
        }
    }

    /**
     * Record an order timestamp for rate-limiting calculation.
     */
    public void recordOrderAttempt(UUID userId) {
        Instant now = Instant.now();
        userOrderTimestamps.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>()).addLast(now);
    }

    /**
     * 3. Verify maximum 50 open positions limit.
     */
    public void checkOpenPositionLimit(UUID userId, String symbol, String side) {
        if (!"BUY".equalsIgnoreCase(side)) {
            // SELL orders reduce or close existing positions, no open position limit constraint
            return;
        }

        // Check if user already holds this symbol
        Optional<Holding> existingHolding = holdingRepo.findByUserIdAndSymbol(userId, symbol);
        boolean alreadyHolds = existingHolding.isPresent() && existingHolding.get().getQuantity() > 0;

        if (!alreadyHolds) {
            long currentOpenPositions = holdingRepo.countOpenPositionsByUserId(userId);
            if (currentOpenPositions >= MAX_OPEN_POSITIONS) {
                log.warn("Risk rejection for user {}: open position limit reached ({}/{})",
                        userId, currentOpenPositions, MAX_OPEN_POSITIONS);
                throw new PositionLimitExceededException(
                        "Open position limit reached: Maximum " + MAX_OPEN_POSITIONS + " concurrent active positions allowed.");
            }
        }
    }

    /**
     * 4. Verify user has sufficient cash ledger margin balance for BUY orders.
     */
    public void checkMargin(UUID userId, String side, int quantity, BigDecimal price) {
        if (!"BUY".equalsIgnoreCase(side)) {
            return;
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Margin check skipped: order price not specified or non-positive");
            return;
        }

        BigDecimal requiredMargin = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal availableBalance = getAvailableLedgerBalance(userId);

        if (availableBalance.compareTo(requiredMargin) < 0) {
            log.warn("Risk rejection for user {}: insufficient margin. Required: {}, Available: {}",
                    userId, requiredMargin, availableBalance);
            throw new InsufficientMarginException(
                    String.format("Insufficient margin: Required ₹%.2f, but available cash balance is ₹%.2f",
                            requiredMargin.doubleValue(), availableBalance.doubleValue()));
        }
    }

    /**
     * Calculate current user available cash balance from ledger.
     */
    public BigDecimal getAvailableLedgerBalance(UUID userId) {
        Double balance = ledgerRepo.calculateBalance(userId);
        if (balance == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(balance);
    }

    /**
     * Clear rate-limit state for a user (useful in tests).
     */
    public void resetVelocity(UUID userId) {
        userOrderTimestamps.remove(userId);
    }
}
