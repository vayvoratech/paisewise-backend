package in.sapphirus.rupee.practice.service;

import in.sapphirus.rupee.practice.domain.*;
import in.sapphirus.rupee.practice.quote.RedisQuoteService;
import in.sapphirus.rupee.practice.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private PaperAccount getOrCreateAccount(UUID userId) {
        return paperAccounts.findById(userId)
                .orElseGet(() -> paperAccounts.save(new PaperAccount(userId)));
    }

    private void resetIfEligible(PaperAccount account) {
        Instant now = Instant.now();

        if (!now.isBefore(account.getLastResetAt().plus(30, ChronoUnit.DAYS))) {
            account.setBalance(new BigDecimal("100000.00"));
            account.setLastResetAt(now);
            paperAccounts.save(account);
        }
    }

    private void validateBuyBalance(PaperAccount account, BigDecimal requiredAmount) {
        if (account.getBalance().compareTo(requiredAmount) < 0) {
            throw new IllegalArgumentException("Insufficient paper trading balance");
        }
    }

    private BigDecimal calculateOrderValue(BigDecimal price, int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    @Transactional
    public Order executeMarketBuy(UUID userId, Stock stock, int quantity) {
        BigDecimal marketPrice = quotes.getQuote(stock.getSymbol());

        if (marketPrice == null) {
            throw new IllegalStateException("Quote not available for " + stock.getSymbol());
        }

        BigDecimal orderValue = calculateOrderValue(marketPrice, quantity);

        PaperAccount account = getOrCreateAccount(userId);
        resetIfEligible(account);
        validateBuyBalance(account, orderValue);

        account.setBalance(account.getBalance().subtract(orderValue));
        paperAccounts.save(account);

        PaperPosition position = getOrCreatePosition(userId, stock.getSymbol());
        position.setQuantity(position.getQuantity() + quantity);
        paperPositions.save(position);

        String clientOrderId = userId + "-" + System.currentTimeMillis();

        Order order = new Order(
                userId,
                clientOrderId,
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
                .orElseGet(() ->
                        paperPositions.save(new PaperPosition(userId, symbol))
                );
    }

    @Transactional
    public Order executeMarketSell(UUID userId, Stock stock, int quantity) {
        BigDecimal marketPrice = quotes.getQuote(stock.getSymbol());

        if (marketPrice == null) {
            throw new IllegalStateException(
                    "Quote not available for " + stock.getSymbol()
            );
        }

        PaperAccount account = getOrCreateAccount(userId);
        resetIfEligible(account);

        PaperPosition position = getOrCreatePosition(
                userId,
                stock.getSymbol()
        );

        if (position.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient paper shares"
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

        String clientOrderId =
                userId + "-" + System.currentTimeMillis();

        Order order = new Order(
                userId,
                clientOrderId,
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
            BigDecimal limitPrice
    ) {
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

        String clientOrderId =
                userId + "-" + System.currentTimeMillis();

        Order order = new Order(
                userId,
                clientOrderId,
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
            BigDecimal limitPrice
    ) {
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

        String clientOrderId =
                userId + "-" + System.currentTimeMillis();

        Order order = new Order(
                userId,
                clientOrderId,
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
                account.getReservedBalance().subtract(reservedAmount)
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
                quantity,
                fillPrice,
                netAmount,
                true
        );

        return trades.save(trade);
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
}