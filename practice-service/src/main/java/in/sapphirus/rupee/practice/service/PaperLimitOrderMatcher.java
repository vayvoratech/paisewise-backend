package in.sapphirus.rupee.practice.service;

import in.sapphirus.rupee.practice.domain.Order;
import in.sapphirus.rupee.practice.quote.RedisQuoteService;
import in.sapphirus.rupee.practice.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class PaperLimitOrderMatcher {
    private static final Logger log = LoggerFactory.getLogger(PaperLimitOrderMatcher.class);

    public static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Kolkata");
    public static final LocalTime MARKET_OPEN = LocalTime.of(9, 15);
    public static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    private final OrderRepository orders;
    private final RedisQuoteService quotes;
    private final PaperOrderService paperOrderService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public PaperLimitOrderMatcher(
            OrderRepository orders,
            RedisQuoteService quotes,
            PaperOrderService paperOrderService
    ) {
        this(orders, quotes, paperOrderService, Clock.system(MARKET_ZONE));
    }

    public PaperLimitOrderMatcher(
            OrderRepository orders,
            RedisQuoteService quotes,
            PaperOrderService paperOrderService,
            Clock clock
    ) {
        this.orders = orders;
        this.quotes = quotes;
        this.paperOrderService = paperOrderService;
        this.clock = clock;
    }

    @Scheduled(fixedRate = 500)
    @Transactional
    public void matchOpenOrders() {
        LocalDateTime now = LocalDateTime.now(clock);

        if (!isMarketOpen(now)) {
            return;
        }

        List<Order> openOrders =
                orders.findByIsPaperTrueAndOrderTypeAndStatus(
                        "LIMIT",
                        "OPEN"
                );

        if (openOrders.isEmpty()) {
            return;
        }

        for (Order order : openOrders) {
            try {
                matchSingleOrder(order);
            } catch (Exception e) {
                log.error("Failed to match limit order id={}: {}", order.getId(), e.getMessage(), e);
            }
        }
    }

    public boolean matchSingleOrder(Order order) {
        if (!"OPEN".equals(order.getStatus()) || !"LIMIT".equals(order.getOrderType())) {
            return false;
        }

        BigDecimal marketPrice = quotes.getQuote(order.getSymbol());
        if (marketPrice == null) {
            return false;
        }

        BigDecimal limitPrice = order.getPrice();
        if (limitPrice == null || limitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        if ("BUY".equals(order.getSide()) && marketPrice.compareTo(limitPrice) <= 0) {
            log.info("Matching LIMIT BUY order id={} symbol={} limitPrice={} marketPrice={}",
                    order.getId(), order.getSymbol(), limitPrice, marketPrice);
            paperOrderService.executeLimitBuy(order, marketPrice);
            return true;
        } else if ("SELL".equals(order.getSide()) && marketPrice.compareTo(limitPrice) >= 0) {
            log.info("Matching LIMIT SELL order id={} symbol={} limitPrice={} marketPrice={}",
                    order.getId(), order.getSymbol(), limitPrice, marketPrice);
            paperOrderService.executeLimitSell(order, marketPrice);
            return true;
        }

        return false;
    }

    public boolean isMarketOpen(LocalDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime time = now.toLocalTime();
        return !time.isBefore(MARKET_OPEN) && !time.isAfter(MARKET_CLOSE);
    }
}