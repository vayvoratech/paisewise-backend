package in.sapphirus.rupee.practice.service;

import in.sapphirus.rupee.practice.domain.*;
import in.sapphirus.rupee.practice.quote.RedisQuoteService;
import in.sapphirus.rupee.practice.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaperOrderServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final PaperAccountRepository paperAccountRepository = mock(PaperAccountRepository.class);
    private final PaperPositionRepository paperPositionRepository = mock(PaperPositionRepository.class);
    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final StockRepository stockRepository = mock(StockRepository.class);
    private final RedisQuoteService redisQuoteService = mock(RedisQuoteService.class);

    private PaperOrderService service;

    private final UUID userId = UUID.randomUUID();
    private final Stock stock = new Stock("RELIANCE", "Reliance Industries Ltd.", 2500.0, 1.5, "🛢️", "[]");

    @BeforeEach
    void setUp() {
        reset(orderRepository, paperAccountRepository, paperPositionRepository, tradeRepository, stockRepository, redisQuoteService);
        service = new PaperOrderService(
                orderRepository,
                paperAccountRepository,
                paperPositionRepository,
                tradeRepository,
                stockRepository,
                redisQuoteService
        );

        when(paperAccountRepository.save(any(PaperAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paperPositionRepository.save(any(PaperPosition.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getOrCreateAccount_createsFreshWith100kBalance() {
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.empty());

        PaperAccount account = service.getOrCreateAccount(userId);

        assertThat(account).isNotNull();
        assertThat(account.getUserId()).isEqualTo(userId);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(account.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(paperAccountRepository).save(any(PaperAccount.class));
    }

    @Test
    void executeMarketBuy_success() {
        PaperAccount account = new PaperAccount(userId);
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));
        when(paperPositionRepository.findByUserIdAndSymbol(userId, "RELIANCE")).thenReturn(Optional.empty());
        when(redisQuoteService.getQuote("RELIANCE")).thenReturn(new BigDecimal("2500.00"));

        Order order = service.executeMarketBuy(userId, stock, 10, "client-1");

        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo("COMPLETE");
        assertThat(order.getFilledQty()).isEqualTo(10);
        assertThat(order.getSide()).isEqualTo("BUY");
        assertThat(order.getPrice()).isEqualByComparingTo("2500.00");

        // 100,000 - (2,500 * 10) = 75,000
        assertThat(account.getBalance()).isEqualByComparingTo("75000.00");

        ArgumentCaptor<PaperPosition> posCaptor = ArgumentCaptor.forClass(PaperPosition.class);
        verify(paperPositionRepository).save(posCaptor.capture());
        assertThat(posCaptor.getValue().getQuantity()).isEqualTo(10);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(tradeCaptor.capture());
        assertThat(tradeCaptor.getValue().getFillPrice()).isEqualByComparingTo("2500.00");
        assertThat(tradeCaptor.getValue().getFillQty()).isEqualTo(10);
    }

    @Test
    void executeMarketBuy_insufficientBalance_throwsException() {
        PaperAccount account = new PaperAccount(userId);
        account.setBalance(new BigDecimal("1000.00"));
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));
        when(redisQuoteService.getQuote("RELIANCE")).thenReturn(new BigDecimal("2500.00"));

        assertThatThrownBy(() -> service.executeMarketBuy(userId, stock, 1, "client-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient available paper trading balance");
    }

    @Test
    void executeMarketBuy_invalidQuantity_throwsException() {
        assertThatThrownBy(() -> service.executeMarketBuy(userId, stock, 0, "client-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than zero");
    }

    @Test
    void executeMarketBuy_fallsBackToCatalogPrice_whenRedisNull() {
        PaperAccount account = new PaperAccount(userId);
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));
        when(paperPositionRepository.findByUserIdAndSymbol(userId, "RELIANCE")).thenReturn(Optional.empty());
        when(redisQuoteService.getQuote("RELIANCE")).thenReturn(null);

        Order order = service.executeMarketBuy(userId, stock, 2, "client-1");

        assertThat(order.getPrice()).isEqualByComparingTo("2500.0");
        assertThat(account.getBalance()).isEqualByComparingTo("95000.00");
    }

    @Test
    void executeMarketSell_success() {
        PaperAccount account = new PaperAccount(userId);
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        PaperPosition position = new PaperPosition(userId, "RELIANCE");
        position.setQuantity(10);
        position.setReservedQuantity(0);
        when(paperPositionRepository.findByUserIdAndSymbol(userId, "RELIANCE")).thenReturn(Optional.of(position));
        when(redisQuoteService.getQuote("RELIANCE")).thenReturn(new BigDecimal("2600.00"));

        Order order = service.executeMarketSell(userId, stock, 4, "client-2");

        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo("COMPLETE");
        assertThat(order.getFilledQty()).isEqualTo(4);
        assertThat(order.getSide()).isEqualTo("SELL");
        // Balance increases by 4 * 2600 = 10,400 -> 110,400
        assertThat(account.getBalance()).isEqualByComparingTo("110400.00");
        assertThat(position.getQuantity()).isEqualTo(6);
    }

    @Test
    void executeMarketSell_insufficientShares_throwsException() {
        PaperAccount account = new PaperAccount(userId);
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        PaperPosition position = new PaperPosition(userId, "RELIANCE");
        position.setQuantity(2);
        when(paperPositionRepository.findByUserIdAndSymbol(userId, "RELIANCE")).thenReturn(Optional.of(position));
        when(redisQuoteService.getQuote("RELIANCE")).thenReturn(new BigDecimal("2600.00"));

        assertThatThrownBy(() -> service.executeMarketSell(userId, stock, 5, "client-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient available paper shares");
    }

    @Test
    void executeMarketSell_reservedSharesNotAvailable_throwsException() {
        PaperAccount account = new PaperAccount(userId);
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        PaperPosition position = new PaperPosition(userId, "RELIANCE");
        position.setQuantity(10);
        position.setReservedQuantity(8); // only 2 available
        when(paperPositionRepository.findByUserIdAndSymbol(userId, "RELIANCE")).thenReturn(Optional.of(position));
        when(redisQuoteService.getQuote("RELIANCE")).thenReturn(new BigDecimal("2600.00"));

        assertThatThrownBy(() -> service.executeMarketSell(userId, stock, 3, "client-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient available paper shares");
    }

    @Test
    void placeLimitBuy_success() {
        PaperAccount account = new PaperAccount(userId);
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        BigDecimal limitPrice = new BigDecimal("2400.00");
        Order order = service.placeLimitBuy(userId, stock, 10, limitPrice, "client-limit-buy");

        assertThat(order.getStatus()).isEqualTo("OPEN");
        assertThat(order.getOrderType()).isEqualTo("LIMIT");
        assertThat(order.getSide()).isEqualTo("BUY");
        assertThat(order.getPrice()).isEqualByComparingTo(limitPrice);
        assertThat(order.getFilledQty()).isEqualTo(0);

        // Reserved balance = 10 * 2400 = 24,000
        assertThat(account.getReservedBalance()).isEqualByComparingTo("24000.00");
        // Balance itself remains 100,000 until execution
        assertThat(account.getBalance()).isEqualByComparingTo("100000.00");
    }

    @Test
    void placeLimitBuy_insufficientAvailableBalance_throwsException() {
        PaperAccount account = new PaperAccount(userId);
        account.setBalance(new BigDecimal("20000.00"));
        account.setReservedBalance(new BigDecimal("15000.00")); // available is 5000
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        BigDecimal limitPrice = new BigDecimal("2000.00");
        // Cost: 3 * 2000 = 6000 > 5000
        assertThatThrownBy(() -> service.placeLimitBuy(userId, stock, 3, limitPrice, "client-3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient available paper trading balance");
    }

    @Test
    void placeLimitSell_success() {
        PaperAccount account = new PaperAccount(userId);
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        PaperPosition position = new PaperPosition(userId, "RELIANCE");
        position.setQuantity(20);
        position.setReservedQuantity(5);
        when(paperPositionRepository.findByUserIdAndSymbol(userId, "RELIANCE")).thenReturn(Optional.of(position));

        BigDecimal limitPrice = new BigDecimal("2700.00");
        Order order = service.placeLimitSell(userId, stock, 10, limitPrice, "client-limit-sell");

        assertThat(order.getStatus()).isEqualTo("OPEN");
        assertThat(order.getOrderType()).isEqualTo("LIMIT");
        assertThat(order.getSide()).isEqualTo("SELL");
        assertThat(order.getPrice()).isEqualByComparingTo(limitPrice);

        // Reserved quantity becomes 5 + 10 = 15
        assertThat(position.getReservedQuantity()).isEqualTo(15);
    }

    @Test
    void placeLimitSell_insufficientAvailableShares_throwsException() {
        PaperPosition position = new PaperPosition(userId, "RELIANCE");
        position.setQuantity(10);
        position.setReservedQuantity(8); // only 2 available
        when(paperPositionRepository.findByUserIdAndSymbol(userId, "RELIANCE")).thenReturn(Optional.of(position));

        BigDecimal limitPrice = new BigDecimal("2700.00");
        assertThatThrownBy(() -> service.placeLimitSell(userId, stock, 5, limitPrice, "client-sell-fail"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient available paper shares");
    }

    @Test
    void executeLimitBuy_success_withPriceImprovement() {
        PaperAccount account = new PaperAccount(userId);
        account.setBalance(new BigDecimal("100000.00"));
        account.setReservedBalance(new BigDecimal("25000.00")); // 10 * 2500 limit price
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        PaperPosition position = new PaperPosition(userId, "RELIANCE");
        position.setQuantity(0);
        when(paperPositionRepository.findByUserIdAndSymbol(userId, "RELIANCE")).thenReturn(Optional.of(position));

        Order order = new Order(userId, "order-limit", "RELIANCE", "NSE", "BUY", "LIMIT", "CNC", 10, true);
        order.setPrice(new BigDecimal("2500.00"));
        order.setStatus("OPEN");

        // Executed at 2400 (better than limit price 2500)
        BigDecimal marketPrice = new BigDecimal("2400.00");
        Order executed = service.executeLimitBuy(order, marketPrice);

        assertThat(executed.getStatus()).isEqualTo("COMPLETE");
        assertThat(executed.getFilledQty()).isEqualTo(10);
        assertThat(executed.getAvgPrice()).isEqualByComparingTo("2400.00");

        // Actual paid: 24,000 -> balance = 100,000 - 24,000 = 76,000
        assertThat(account.getBalance()).isEqualByComparingTo("76000.00");
        // Reserved reduced from 25,000 by 25,000 -> 0
        assertThat(account.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        // Position has 10 shares
        assertThat(position.getQuantity()).isEqualTo(10);

        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void executeLimitSell_success() {
        PaperAccount account = new PaperAccount(userId);
        account.setBalance(new BigDecimal("50000.00"));
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        PaperPosition position = new PaperPosition(userId, "RELIANCE");
        position.setQuantity(10);
        position.setReservedQuantity(10);
        when(paperPositionRepository.findByUserIdAndSymbol(userId, "RELIANCE")).thenReturn(Optional.of(position));

        Order order = new Order(userId, "order-limit-sell", "RELIANCE", "NSE", "SELL", "LIMIT", "CNC", 10, true);
        order.setPrice(new BigDecimal("2600.00"));
        order.setStatus("OPEN");

        BigDecimal marketPrice = new BigDecimal("2650.00");
        Order executed = service.executeLimitSell(order, marketPrice);

        assertThat(executed.getStatus()).isEqualTo("COMPLETE");
        assertThat(executed.getFilledQty()).isEqualTo(10);

        // Balance = 50,000 + 26,500 = 76,500
        assertThat(account.getBalance()).isEqualByComparingTo("76500.00");
        // Position quantity & reserved both 0
        assertThat(position.getQuantity()).isEqualTo(0);
        assertThat(position.getReservedQuantity()).isEqualTo(0);

        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void cancelOrder_limitBuy_releasesReservedBalance() {
        PaperAccount account = new PaperAccount(userId);
        account.setBalance(new BigDecimal("100000.00"));
        account.setReservedBalance(new BigDecimal("25000.00"));
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        UUID orderId = UUID.randomUUID();
        Order order = new Order(userId, "cid-buy", "RELIANCE", "NSE", "BUY", "LIMIT", "CNC", 10, true);
        order.setPrice(new BigDecimal("2500.00"));
        order.setStatus("OPEN");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.cancelOrder(userId, orderId);

        assertThat(order.getStatus()).isEqualTo("CANCELLED");
        // Reserved balance reduced by 25,000 -> 0
        assertThat(account.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(orderRepository).save(order);
        verify(paperAccountRepository).save(account);
    }

    @Test
    void cancelOrder_limitSell_releasesReservedQuantity() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(userId, "cid-sell", "RELIANCE", "NSE", "SELL", "LIMIT", "CNC", 10, true);
        order.setPrice(new BigDecimal("2600.00"));
        order.setStatus("OPEN");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaperPosition position = new PaperPosition(userId, "RELIANCE");
        position.setQuantity(15);
        position.setReservedQuantity(10);
        when(paperPositionRepository.findByUserIdAndSymbol(userId, "RELIANCE")).thenReturn(Optional.of(position));

        service.cancelOrder(userId, orderId);

        assertThat(order.getStatus()).isEqualTo("CANCELLED");
        // Reserved quantity becomes 10 - 10 = 0, total quantity still 15
        assertThat(position.getReservedQuantity()).isEqualTo(0);
        assertThat(position.getQuantity()).isEqualTo(15);
        verify(paperPositionRepository).save(position);
    }

    @Test
    void cancelOrder_forbidden_whenDifferentUser() {
        UUID orderId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        Order order = new Order(otherUser, "cid", "RELIANCE", "NSE", "BUY", "LIMIT", "CNC", 10, true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelOrder(userId, orderId))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode() == HttpStatus.FORBIDDEN);
    }

    @Test
    void cancelOrder_badRequest_whenNotOpen() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(userId, "cid", "RELIANCE", "NSE", "BUY", "LIMIT", "CNC", 10, true);
        order.setStatus("COMPLETE");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelOrder(userId, orderId))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode() == HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetAccount_failsWithin30Days() {
        PaperAccount account = new PaperAccount(userId);
        // Created just now
        account.setLastResetAt(Instant.now().minus(5, ChronoUnit.DAYS));
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.resetAccount(userId))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode() == HttpStatus.BAD_REQUEST)
                .hasMessageContaining("Account can only be reset once every 30 days");
    }

    @Test
    void resetAccount_succeedsAfter30Days() {
        PaperAccount account = new PaperAccount(userId);
        account.setBalance(new BigDecimal("25000.00"));
        account.setReservedBalance(new BigDecimal("10000.00"));
        account.setLastResetAt(Instant.now().minus(31, ChronoUnit.DAYS));
        when(paperAccountRepository.findById(userId)).thenReturn(Optional.of(account));

        Order openOrder = new Order(userId, "cid", "RELIANCE", "NSE", "BUY", "LIMIT", "CNC", 5, true);
        openOrder.setStatus("OPEN");
        when(orderRepository.findByUserIdOrderByPlacedAtDesc(userId)).thenReturn(List.of(openOrder));

        PaperPosition position = new PaperPosition(userId, "RELIANCE");
        position.setQuantity(20);
        position.setReservedQuantity(5);
        when(paperPositionRepository.findByUserId(userId)).thenReturn(List.of(position));

        PaperAccount reset = service.resetAccount(userId);

        assertThat(reset.getBalance()).isEqualByComparingTo("100000.00");
        assertThat(reset.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(openOrder.getStatus()).isEqualTo("CANCELLED");
        assertThat(position.getQuantity()).isEqualTo(0);
        assertThat(position.getReservedQuantity()).isEqualTo(0);
        assertThat(reset.getLastResetAt()).isAfter(Instant.now().minus(5, ChronoUnit.SECONDS));
    }
}
