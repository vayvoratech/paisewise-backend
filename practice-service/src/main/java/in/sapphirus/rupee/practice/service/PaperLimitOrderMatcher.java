package in.sapphirus.rupee.practice.service;

import in.sapphirus.rupee.practice.domain.Order;
import in.sapphirus.rupee.practice.repo.OrderRepository;
import in.sapphirus.rupee.practice.quote.RedisQuoteService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class PaperLimitOrderMatcher {
    private static final ZoneId MARKET_ZONE =
            ZoneId.of("Asia/Kolkata");

    private static final LocalTime MARKET_OPEN =
            LocalTime.of(9, 15);

    private static final LocalTime MARKET_CLOSE =
            LocalTime.of(15, 30);

    private final OrderRepository orders;
    private final RedisQuoteService quotes;
    private final PaperOrderService paperOrderService;

    public PaperLimitOrderMatcher(
            OrderRepository orders,
            RedisQuoteService quotes,
            PaperOrderService paperOrderService
    ) {
        this.orders = orders;
        this.quotes = quotes;
        this.paperOrderService = paperOrderService;
    }

    @Scheduled(fixedRate = 500)
    @Transactional
    public void matchOpenOrders() {

        LocalDateTime now =
                LocalDateTime.now(MARKET_ZONE);

        if (!isMarketOpen(now)) {
            return;
        }

        List<Order> openOrders =
                orders.findByIsPaperTrueAndOrderTypeAndStatus(
                        "LIMIT",
                        "OPEN"
                );

        for (Order order : openOrders) {

            BigDecimal marketPrice =
                    quotes.getQuote(order.getSymbol());

            if (marketPrice == null) {
                continue;
            }

            BigDecimal limitPrice = order.getPrice();

            if (limitPrice == null) {
                continue;
            }

            if ("BUY".equals(order.getSide())
                    && marketPrice.compareTo(limitPrice) <= 0) {

                paperOrderService.executeLimitBuy(
                        order,
                        marketPrice
                );

            } else if ("SELL".equals(order.getSide())
                    && marketPrice.compareTo(limitPrice) >= 0) {

                paperOrderService.executeLimitSell(
                        order,
                        marketPrice
                );
            }
        }
    }

    private boolean isMarketOpen(LocalDateTime now) {

        DayOfWeek day = now.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY ||
                day == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime time = now.toLocalTime();

        return !time.isBefore(MARKET_OPEN)
                && !time.isAfter(MARKET_CLOSE);
    }
}