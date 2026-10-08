package in.sapphirus.rupee.practice.service;

import in.sapphirus.rupee.practice.domain.*;
import in.sapphirus.rupee.practice.quote.RedisQuoteService;
import in.sapphirus.rupee.practice.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class PaperOrderService {
    private final OrderRepository orders;
    private final PaperAccountRepository paperAccounts;
    private final StockRepository stocks;
    private final TradeRepository trades;
    private final RedisQuoteService quotes;
    private final PaperPositionRepository paperPositions;

    public PaperOrderService(
            OrderRepository orders,
            PaperAccountRepository paperAccounts,
            PaperPositionRepository paperPositions,
            TradeRepository trades,
            StockRepository stocks,
            RedisQuoteService quotes
    ) {
        this.orders = orders;
        this.paperAccounts = paperAccounts;
        this.paperPositions = paperPositions;
        this.trades = trades;
        this.stocks = stocks;
        this.quotes = quotes;
    }

    public static final BigDecimal INITIAL_BALANCE = new BigDecimal("100000.00");
    public static final int RESET_COOLDOWN_DAYS = 30;

    public PaperAccount getOrCreateAccount(UUID userId) {
        return paperAccounts.findById(userId)
                .orElseGet(() -> paperAccounts.save(new PaperAccount(userId)));
    }

    private void resetIfEligible(PaperAccount account) {
        Instant now = Instant.now();

        if (!now.isBefore(account.getLastResetAt().plus(RESET_COOLDOWN_DAYS, ChronoUnit.DAYS))) {
            account.setBalance(INITIAL_BALANCE);
            account.setReservedBalance(BigDecimal.ZERO);
            account.setLastResetAt(now);
            paperAccounts.save(account);
        }
    }

    @Transactional
    public PaperAccount resetAccount(UUID userId) {
        PaperAccount account = getOrCreateAccount(userId);
        Instant now = Instant.now();
        Instant nextEligibleReset = account.getLastResetAt().plus(RESET_COOLDOWN_DAYS, ChronoUnit.DAYS);

        if (now.isBefore(nextEligibleReset)) {
            long daysRemaining = ChronoUnit.DAYS.between(now, nextEligibleReset);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account can only be reset once every 30 days. Days remaining: " + Math.max(1, daysRemaining)
            );
        }

        // Cancel pending open orders
        orders.findByUserIdOrderByPlacedAtDesc(userId).stream()
                .filter(o -> "OPEN".equals(o.getStatus()))
                .forEach(openOrder -> {
                    openOrder.setStatus("CANCELLED");
                    orders.save(openOrder);
                });

        // Reset positions
        paperPositions.findByUserId(userId).forEach(position -> {
            position.setQuantity(0);
            position.setReservedQuantity(0);
            paperPositions.save(position);
        });

        account.setBalance(INITIAL_BALANCE);
        account.setReservedBalance(BigDecimal.ZERO);
        account.setLastResetAt(now);
        return paperAccounts.save(account);
    }

    private void validateBuyBalance(PaperAccount account, BigDecimal requiredAmount) {
        if (account.getBalance().compareTo(requiredAmount) < 0) {
            throw new IllegalArgumentException("Insufficient paper trading balance");
        }
    }

    private BigDecimal calculateOrderValue(BigDecimal price, int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    private String resolveClientOrderId(UUID userId, String provided) {
        return (provided != null && !provided.isBlank())
                ? provided
                : userId + "-" + System.currentTimeMillis();
    }

    @Transactional
    public Order executeMarketBuy(UUID userId, Stock stock, int quantity, String clientOrderId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        BigDecimal marketPrice = quotes.getQuote(stock.getSymbol());

        // Fall back to the stock's catalog price when Redis is unavailable or
        // the quote has not been published yet (e.g. Redis just restarted).
        if (marketPrice == null) {
            marketPrice = BigDecimal.valueOf(stock.getPrice());
        }

        BigDecimal orderValue = calculateOrderValue(marketPrice, quantity);

        PaperAccount account = getOrCreateAccount(userId);
        resetIfEligible(account);
        validateAvailableBuyBalance(account, orderValue);

        account.setBalance(account.getBalance().subtract(orderValue));
        paperAccounts.save(account);

        PaperPosition position = getOrCreatePosition(userId, stock.getSymbol());
        position.setQuantity(position.getQuantity() + quantity);
        paperPositions.save(position);

        Order order = new Order(
                userId,
                resolveClientOrderId(userId, clientOrderId),
                stock.getSymbol(),
                "NSE",
                "BUY",
                "MARKET",
                "CNC",
                quantity,
                true
        );

        order.setStatus("COMPLETE");
        order.setFilledQty(quantity);
        order.setPrice(marketPrice);
        order.setAvgPrice(marketPrice);

        Order savedOrder = orders.save(order);

        recordTrade(
                savedOrder,
                userId,
                stock.getSymbol(),
                "BUY",
                quantity,
                marketPrice
        );

        return savedOrder;
    }

    private PaperPosition getOrCreatePosition(UUID userId, String symbol) {
        return paperPositions.findByUserIdAndSymbol(userId, symbol)
                .orElseGet(() -> new PaperPosition(userId, symbol));
    }

    @Transactional
    public Order executeMarketSell(UUID userId, Stock stock, int quantity, String clientOrderId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        BigDecimal marketPrice = quotes.getQuote(stock.getSymbol());

        // Fall back to the stock's catalog price when Redis is unavailable.
        if (marketPrice == null) {
            marketPrice = BigDecimal.valueOf(stock.getPrice());
        }

        PaperAccount account = getOrCreateAccount(userId);
        resetIfEligible(account);

        PaperPosition position = getOrCreatePosition(
                userId,
                stock.getSymbol()
        );

        int availableQuantity = position.getQuantity() - position.getReservedQuantity();
        if (availableQuantity < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient available paper shares"
            );
        }

        BigDecimal orderValue = calculateOrderValue(
                marketPrice,
                quantity
        );

        account.setBalance(
                account.getBalance().add(orderValue)
        );
        paperAccounts.save(account);

        position.setQuantity(
                position.getQuantity() - quantity
        );
        paperPositions.save(position);

        Order order = new Order(
                userId,
                resolveClientOrderId(userId, clientOrderId),
                stock.getSymbol(),
                "NSE",
                "SELL",
                "MARKET",
                "CNC",
                quantity,
                true
        );

        order.setStatus("COMPLETE");
        order.setFilledQty(quantity);
        order.setPrice(marketPrice);
        order.setAvgPrice(marketPrice);

        Order savedOrder = orders.save(order);

        recordTrade(
                savedOrder,
                userId,
                stock.getSymbol(),
                "SELL",
                quantity,
                marketPrice
        );

        return savedOrder;
    }

    @Transactional
    public Order placeLimitBuy(
            UUID userId,
            Stock stock,
            int quantity,
            BigDecimal limitPrice,
            String clientOrderId
    ) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (limitPrice == null || limitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Limit price must be greater than zero");
        }

        BigDecimal orderValue = calculateOrderValue(limitPrice, quantity);

        PaperAccount account = getOrCreateAccount(userId);
        resetIfEligible(account);

        validateAvailableBuyBalance(account, orderValue);

        // Reserve the money so it cannot be used by another pending order.
        account.setReservedBalance(
                account.getReservedBalance().add(orderValue)
        );
        paperAccounts.save(account);

        Order order = new Order(
                userId,
                resolveClientOrderId(userId, clientOrderId),
                stock.getSymbol(),
                "NSE",
                "BUY",
                "LIMIT",
                "CNC",
                quantity,
                true
        );

        order.setPrice(limitPrice);
        order.setFilledQty(0);
        order.setStatus("OPEN");

        return orders.save(order);
    }

    @Transactional
    public Order placeLimitSell(
            UUID userId,
            Stock stock,
            int quantity,
            BigDecimal limitPrice,
            String clientOrderId
    ) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (limitPrice == null || limitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Limit price must be greater than zero"
            );
        }

        PaperPosition position =
                getOrCreatePosition(userId, stock.getSymbol());

        int availableQuantity =
                position.getQuantity() - position.getReservedQuantity();

        if (availableQuantity < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient available paper shares"
            );
        }

        position.setReservedQuantity(
                position.getReservedQuantity() + quantity
        );

        paperPositions.save(position);

        Order order = new Order(
                userId,
                resolveClientOrderId(userId, clientOrderId),
                stock.getSymbol(),
                "NSE",
                "SELL",
                "LIMIT",
                "CNC",
                quantity,
                true
        );

        order.setPrice(limitPrice);
        order.setFilledQty(0);
        order.setStatus("OPEN");

        return orders.save(order);
    }

    @Transactional
    public Order executeLimitBuy(
            Order order,
            BigDecimal marketPrice
    ) {
        if (!"BUY".equals(order.getSide())) {
            throw new IllegalArgumentException("Order is not a BUY order");
        }

        if (!"LIMIT".equals(order.getOrderType())) {
            throw new IllegalArgumentException("Order is not a LIMIT order");
        }

        if (!"OPEN".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not OPEN");
        }

        if (marketPrice == null || marketPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid market price");
        }

        UUID userId = order.getUserId();

        PaperAccount account = getOrCreateAccount(userId);
        resetIfEligible(account);

        int quantity = order.getQuantity();

        BigDecimal actualOrderValue =
                calculateOrderValue(marketPrice, quantity);

        BigDecimal reservedAmount =
                calculateOrderValue(order.getPrice(), quantity);

        if (account.getReservedBalance().compareTo(reservedAmount) < 0) {
            throw new IllegalStateException(
                    "Insufficient reserved balance for order"
            );
        }

        // Remove the reservation.
        account.setReservedBalance(
                account.getReservedBalance().subtract(reservedAmount).max(BigDecimal.ZERO)
        );

        // Pay the actual execution amount.
        account.setBalance(
                account.getBalance().subtract(actualOrderValue)
        );

        paperAccounts.save(account);

        // Add purchased shares.
        PaperPosition position =
                getOrCreatePosition(userId, order.getSymbol());

        position.setQuantity(
                position.getQuantity() + quantity
        );

        paperPositions.save(position);

        // Complete the order.
        order.setFilledQty(quantity);
        order.setPrice(marketPrice);
        order.setAvgPrice(marketPrice);
        order.setStatus("COMPLETE");

        Order savedOrder = orders.save(order);

        recordTrade(
                savedOrder,
                userId,
                order.getSymbol(),
                "BUY",
                quantity,
                marketPrice
        );

        return savedOrder;
    }

    @Transactional
    public Order executeLimitSell(
            Order order,
            BigDecimal marketPrice
    ) {
        if (!"SELL".equals(order.getSide())) {
            throw new IllegalArgumentException("Order is not a SELL order");
        }

        if (!"LIMIT".equals(order.getOrderType())) {
            throw new IllegalArgumentException("Order is not a LIMIT order");
        }

        if (!"OPEN".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not OPEN");
        }

        if (marketPrice == null || marketPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid market price");
        }

        UUID userId = order.getUserId();
        int quantity = order.getQuantity();

        PaperPosition position =
                getOrCreatePosition(userId, order.getSymbol());

        if (position.getReservedQuantity() < quantity) {
            throw new IllegalStateException(
                    "Insufficient reserved paper shares"
            );
        }

        if (position.getQuantity() < quantity) {
            throw new IllegalStateException(
                    "Insufficient paper shares"
            );
        }

        BigDecimal orderValue =
                calculateOrderValue(marketPrice, quantity);

        // Remove the shares from the user's actual position.
        position.setQuantity(
                position.getQuantity() - quantity
        );

        // Release the shares that were reserved for this order.
        position.setReservedQuantity(
                position.getReservedQuantity() - quantity
        );

        paperPositions.save(position);

        // Add the sale proceeds to the paper balance.
        PaperAccount account = getOrCreateAccount(userId);
        resetIfEligible(account);

        account.setBalance(
                account.getBalance().add(orderValue)
        );

        paperAccounts.save(account);

        // Complete the order.
        order.setFilledQty(quantity);
        order.setPrice(marketPrice);
        order.setAvgPrice(marketPrice);
        order.setStatus("COMPLETE");

        Order savedOrder = orders.save(order);

        recordTrade(
                savedOrder,
                userId,
                order.getSymbol(),
                "SELL",
                quantity,
                marketPrice
        );

        return savedOrder;
    }

    private Trade recordTrade(
            Order order,
            UUID userId,
            String symbol,
            String side,
            int quantity,
            BigDecimal fillPrice
    ) {
        BigDecimal netAmount = calculateOrderValue(fillPrice, quantity);

        Trade trade = new Trade(
                order.getId(),
                userId,
                symbol,
                "NSE",
                side,
                quantity,
                fillPrice,
                netAmount,
                true
        );

        return trades.save(trade);
    }

    public BigDecimal getAvailableBalance(UUID userId) {
        PaperAccount account = getOrCreateAccount(userId);
        return getAvailableBalance(account);
    }

    private BigDecimal getAvailableBalance(PaperAccount account) {
        return account.getBalance()
                .subtract(account.getReservedBalance());
    }

    private void validateAvailableBuyBalance(
            PaperAccount account,
            BigDecimal requiredAmount
    ) {
        if (getAvailableBalance(account).compareTo(requiredAmount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient available paper trading balance"
            );
        }
    }

    @Transactional
    public void cancelOrder(UUID userId, UUID orderId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        }

        if (!"OPEN".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not OPEN");
        }

        order.setStatus("CANCELLED");
        orders.save(order);

        // Release reserved resources
        if ("BUY".equals(order.getSide()) && "LIMIT".equals(order.getOrderType())) {
            PaperAccount account = getOrCreateAccount(userId);
            BigDecimal reserved = calculateOrderValue(order.getPrice(), order.getQuantity());
            BigDecimal newReserved = account.getReservedBalance().subtract(reserved);
            account.setReservedBalance(newReserved.max(BigDecimal.ZERO));
            paperAccounts.save(account);
        } else if ("SELL".equals(order.getSide()) && "LIMIT".equals(order.getOrderType())) {
            PaperPosition position = paperPositions.findByUserIdAndSymbol(userId, order.getSymbol())
                    .orElse(null);
            if (position != null) {
                int newReserved = Math.max(0, position.getReservedQuantity() - order.getQuantity());
                position.setReservedQuantity(newReserved);
                paperPositions.save(position);
            }
        }
    }
}