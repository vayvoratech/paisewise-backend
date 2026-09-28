package in.sapphirus.rupee.practice.service;

import in.sapphirus.rupee.practice.domain.Order;
import in.sapphirus.rupee.practice.quote.RedisQuoteService;
import in.sapphirus.rupee.practice.repo.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PaperLimitOrderMatcherTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final RedisQuoteService redisQuoteService = mock(RedisQuoteService.class);
    private final PaperOrderService paperOrderService = mock(PaperOrderService.class);

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Kolkata");

    // Wednesday, 2026-09-30 11:00:00 IST (during market hours)
    private static final Instant WEDNESDAY_11AM =
            ZonedDateTime.of(2026, 9, 30, 11, 0, 0, 0, MARKET_ZONE).toInstant();

    // Wednesday, 2026-09-30 08:30:00 IST (before market open)
    private static final Instant WEDNESDAY_830AM =
            ZonedDateTime.of(2026, 9, 30, 8, 30, 0, 0, MARKET_ZONE).toInstant();

    // Wednesday, 2026-09-30 16:00:00 IST (after market close)
    private static final Instant WEDNESDAY_4PM =
            ZonedDateTime.of(2026, 9, 30, 16, 0, 0, 0, MARKET_ZONE).toInstant();

    // Sunday, 2026-09-27 12:00:00 IST (weekend)
    private static final Instant SUNDAY_NOON =
            ZonedDateTime.of(2026, 9, 27, 12, 0, 0, 0, MARKET_ZONE).toInstant();

    @BeforeEach
    void setUp() {
        reset(orderRepository, redisQuoteService, paperOrderService);
    }

    @Test
    void isMarketOpen_correctlyDetectsHours() {
        PaperLimitOrderMatcher matcher = new PaperLimitOrderMatcher(
                orderRepository, redisQuoteService, paperOrderService, Clock.system(MARKET_ZONE));

        // Weekday during hours
        LocalDateTime duringMarket = LocalDateTime.of(2026, 9, 30, 10, 30);
        assertThat(matcher.isMarketOpen(duringMarket)).isTrue();

        // Exactly market open (09:15)
        LocalDateTime atOpen = LocalDateTime.of(2026, 9, 30, 9, 15);
        assertThat(matcher.isMarketOpen(atOpen)).isTrue();

        // Exactly market close (15:30)
        LocalDateTime atClose = LocalDateTime.of(2026, 9, 30, 15, 30);
        assertThat(matcher.isMarketOpen(atClose)).isTrue();

        // Pre-market (09:14)
        LocalDateTime preMarket = LocalDateTime.of(2026, 9, 30, 9, 14);
        assertThat(matcher.isMarketOpen(preMarket)).isFalse();

        // Post-market (15:31)
        LocalDateTime postMarket = LocalDateTime.of(2026, 9, 30, 15, 31);
        assertThat(matcher.isMarketOpen(postMarket)).isFalse();

        // Saturday (even if time is 11:00)
        LocalDateTime saturday = LocalDateTime.of(2026, 9, 26, 11, 0);
        assertThat(matcher.isMarketOpen(saturday)).isFalse();

        // Sunday
        LocalDateTime sunday = LocalDateTime.of(2026, 9, 27, 11, 0);
        assertThat(matcher.isMarketOpen(sunday)).isFalse();
    }

    @Test
    void matchOpenOrders_skipsExecution_whenMarketClosed_onWeekend() {
        Clock clock = Clock.fixed(SUNDAY_NOON, MARKET_ZONE);
        PaperLimitOrderMatcher matcher = new PaperLimitOrderMatcher(
                orderRepository, redisQuoteService, paperOrderService, clock);

        matcher.matchOpenOrders();

        verify(orderRepository, never()).findByIsPaperTrueAndOrderTypeAndStatus(anyString(), anyString());
    }

    @Test
    void matchOpenOrders_skipsExecution_whenMarketClosed_beforeOpen() {
        Clock clock = Clock.fixed(WEDNESDAY_830AM, MARKET_ZONE);
        PaperLimitOrderMatcher matcher = new PaperLimitOrderMatcher(
                orderRepository, redisQuoteService, paperOrderService, clock);

        matcher.matchOpenOrders();

        verify(orderRepository, never()).findByIsPaperTrueAndOrderTypeAndStatus(anyString(), anyString());
    }

    @Test
    void matchOpenOrders_skipsExecution_whenMarketClosed_afterClose() {
        Clock clock = Clock.fixed(WEDNESDAY_4PM, MARKET_ZONE);
        PaperLimitOrderMatcher matcher = new PaperLimitOrderMatcher(
                orderRepository, redisQuoteService, paperOrderService, clock);

        matcher.matchOpenOrders();

        verify(orderRepository, never()).findByIsPaperTrueAndOrderTypeAndStatus(anyString(), anyString());
    }

    @Test
    void matchOpenOrders_matchesBuyOrder_whenMarketPriceAtOrBelowLimit() {
        Clock clock = Clock.fixed(WEDNESDAY_11AM, MARKET_ZONE);
        PaperLimitOrderMatcher matcher = new PaperLimitOrderMatcher(
                orderRepository, redisQuoteService, paperOrderService, clock);

        Order buyOrder = new Order(UUID.randomUUID(), "cid-buy", "RELIANCE", "NSE", "BUY", "LIMIT", "CNC", 10, true);
        buyOrder.setPrice(new BigDecimal("2500.00"));
        buyOrder.setStatus("OPEN");

        when(orderRepository.findByIsPaperTrueAndOrderTypeAndStatus("LIMIT", "OPEN"))
                .thenReturn(List.of(buyOrder));
        when(redisQuoteService.getQuote("RELIANCE")).thenReturn(new BigDecimal("2480.00"));

        matcher.matchOpenOrders();

        verify(paperOrderService, times(1)).executeLimitBuy(buyOrder, new BigDecimal("2480.00"));
        verify(paperOrderService, never()).executeLimitSell(any(), any());
    }

    @Test
    void matchOpenOrders_doesNotMatchBuyOrder_whenMarketPriceAboveLimit() {
        Clock clock = Clock.fixed(WEDNESDAY_11AM, MARKET_ZONE);
        PaperLimitOrderMatcher matcher = new PaperLimitOrderMatcher(
                orderRepository, redisQuoteService, paperOrderService, clock);

        Order buyOrder = new Order(UUID.randomUUID(), "cid-buy", "RELIANCE", "NSE", "BUY", "LIMIT", "CNC", 10, true);
        buyOrder.setPrice(new BigDecimal("2500.00"));
        buyOrder.setStatus("OPEN");

        when(orderRepository.findByIsPaperTrueAndOrderTypeAndStatus("LIMIT", "OPEN"))
                .thenReturn(List.of(buyOrder));
        // Quote is higher than limit price -> no match
        when(redisQuoteService.getQuote("RELIANCE")).thenReturn(new BigDecimal("2520.00"));

        matcher.matchOpenOrders();

        verify(paperOrderService, never()).executeLimitBuy(any(), any());
    }

    @Test
    void matchOpenOrders_matchesSellOrder_whenMarketPriceAtOrAboveLimit() {
        Clock clock = Clock.fixed(WEDNESDAY_11AM, MARKET_ZONE);
        PaperLimitOrderMatcher matcher = new PaperLimitOrderMatcher(
                orderRepository, redisQuoteService, paperOrderService, clock);

        Order sellOrder = new Order(UUID.randomUUID(), "cid-sell", "TCS", "NSE", "SELL", "LIMIT", "CNC", 5, true);
        sellOrder.setPrice(new BigDecimal("3500.00"));
        sellOrder.setStatus("OPEN");

        when(orderRepository.findByIsPaperTrueAndOrderTypeAndStatus("LIMIT", "OPEN"))
                .thenReturn(List.of(sellOrder));
        when(redisQuoteService.getQuote("TCS")).thenReturn(new BigDecimal("3510.00"));

        matcher.matchOpenOrders();

        verify(paperOrderService, times(1)).executeLimitSell(sellOrder, new BigDecimal("3510.00"));
        verify(paperOrderService, never()).executeLimitBuy(any(), any());
    }

    @Test
    void matchOpenOrders_doesNotMatchSellOrder_whenMarketPriceBelowLimit() {
        Clock clock = Clock.fixed(WEDNESDAY_11AM, MARKET_ZONE);
        PaperLimitOrderMatcher matcher = new PaperLimitOrderMatcher(
                orderRepository, redisQuoteService, paperOrderService, clock);

        Order sellOrder = new Order(UUID.randomUUID(), "cid-sell", "TCS", "NSE", "SELL", "LIMIT", "CNC", 5, true);
        sellOrder.setPrice(new BigDecimal("3500.00"));
        sellOrder.setStatus("OPEN");

        when(orderRepository.findByIsPaperTrueAndOrderTypeAndStatus("LIMIT", "OPEN"))
                .thenReturn(List.of(sellOrder));
        // Quote is lower than sell limit -> no match
        when(redisQuoteService.getQuote("TCS")).thenReturn(new BigDecimal("3490.00"));

        matcher.matchOpenOrders();

        verify(paperOrderService, never()).executeLimitSell(any(), any());
    }

    @Test
    void matchOpenOrders_skipsOrder_whenQuoteNull() {
        Clock clock = Clock.fixed(WEDNESDAY_11AM, MARKET_ZONE);
        PaperLimitOrderMatcher matcher = new PaperLimitOrderMatcher(
                orderRepository, redisQuoteService, paperOrderService, clock);

        Order order = new Order(UUID.randomUUID(), "cid", "INFY", "NSE", "BUY", "LIMIT", "CNC", 10, true);
        order.setPrice(new BigDecimal("1800.00"));
        order.setStatus("OPEN");

        when(orderRepository.findByIsPaperTrueAndOrderTypeAndStatus("LIMIT", "OPEN"))
                .thenReturn(List.of(order));
        when(redisQuoteService.getQuote("INFY")).thenReturn(null);

        matcher.matchOpenOrders();

        verify(paperOrderService, never()).executeLimitBuy(any(), any());
        verify(paperOrderService, never()).executeLimitSell(any(), any());
    }

    @Test
    void matchOpenOrders_continuesProcessingRemainingOrders_whenOneThrows() {
        Clock clock = Clock.fixed(WEDNESDAY_11AM, MARKET_ZONE);
        PaperLimitOrderMatcher matcher = new PaperLimitOrderMatcher(
                orderRepository, redisQuoteService, paperOrderService, clock);

        Order order1 = new Order(UUID.randomUUID(), "cid-1", "RELIANCE", "NSE", "BUY", "LIMIT", "CNC", 10, true);
        order1.setPrice(new BigDecimal("2500.00"));
        order1.setStatus("OPEN");

        Order order2 = new Order(UUID.randomUUID(), "cid-2", "TCS", "NSE", "BUY", "LIMIT", "CNC", 5, true);
        order2.setPrice(new BigDecimal("3500.00"));
        order2.setStatus("OPEN");

        when(orderRepository.findByIsPaperTrueAndOrderTypeAndStatus("LIMIT", "OPEN"))
                .thenReturn(List.of(order1, order2));

        when(redisQuoteService.getQuote("RELIANCE")).thenReturn(new BigDecimal("2490.00"));
        when(redisQuoteService.getQuote("TCS")).thenReturn(new BigDecimal("3480.00"));

        // Order 1 throws an unexpected exception
        doThrow(new RuntimeException("Database timeout")).when(paperOrderService).executeLimitBuy(eq(order1), any());

        // Run matching
        matcher.matchOpenOrders();

        // Verify order 1 was attempted and order 2 was also executed despite order 1 failure
        verify(paperOrderService, times(1)).executeLimitBuy(order1, new BigDecimal("2490.00"));
        verify(paperOrderService, times(1)).executeLimitBuy(order2, new BigDecimal("3480.00"));
    }
}
