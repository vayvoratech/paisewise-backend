package in.sapphirus.rupee.practice.quote;

import in.sapphirus.rupee.practice.domain.Stock;
import in.sapphirus.rupee.practice.repo.StockRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RedisQuotePublisher {
    private final StockRepository stocks;
    private final RedisQuoteService redisQuoteService;

    public RedisQuotePublisher(
            StockRepository stocks,
            RedisQuoteService redisQuoteService
    ) {
        this.stocks = stocks;
        this.redisQuoteService = redisQuoteService;
    }

    @Scheduled(fixedRate = 1000)
    public void publishQuotes() {
        for (Stock stock : stocks.findAll()) {
            redisQuoteService.setQuote(
                    stock.getSymbol(),
                    BigDecimal.valueOf(stock.getPrice())
            );
        }
    }
}