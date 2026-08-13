package in.sapphirus.rupee.market.api;

import in.sapphirus.rupee.market.domain.Symbol;
import in.sapphirus.rupee.market.dto.CandleDto;
import in.sapphirus.rupee.market.dto.IndexQuoteDto;
import in.sapphirus.rupee.market.dto.StockQuoteDto;
import in.sapphirus.rupee.market.service.MarketDataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/market")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/quote")
    public StockQuoteDto getQuote(@RequestParam String symbol) {
        return marketDataService.getQuote(symbol);
    }

    @PostMapping("/quotes")
    public List<StockQuoteDto> getQuotes(@RequestBody List<String> symbols) {
        return marketDataService.getQuotes(symbols);
    }

    @GetMapping("/candles")
    public List<CandleDto> getCandles(
            @RequestParam String symbol,
            @RequestParam String resolution,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return marketDataService.getCandles(symbol, resolution, from, to);
    }

    @GetMapping("/search")
    public List<Symbol> searchSymbols(@RequestParam String query) {
        return marketDataService.searchSymbols(query);
    }

    @GetMapping("/indices")
    public List<IndexQuoteDto> getIndices() {
        return marketDataService.getIndices();
    }
}
