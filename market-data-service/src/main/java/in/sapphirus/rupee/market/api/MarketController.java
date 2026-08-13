package in.sapphirus.rupee.market.api;

import in.sapphirus.rupee.market.domain.Symbol;
import in.sapphirus.rupee.market.dto.CandleDto;
import in.sapphirus.rupee.market.dto.IndexQuoteDto;
import in.sapphirus.rupee.market.dto.StockQuoteDto;
import in.sapphirus.rupee.market.service.MarketDataService;
import in.sapphirus.rupee.market.service.SymbolSyncJob;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/market")
public class MarketController {

    private final MarketDataService marketDataService;

    private final SymbolSyncJob symbolSyncJob;

    public MarketController(MarketDataService marketDataService, SymbolSyncJob symbolSyncJob) {
        this.marketDataService = marketDataService;
        this.symbolSyncJob = symbolSyncJob;
    }

    @GetMapping("/quote")
    public ResponseEntity<StockQuoteDto> getQuote(@RequestParam String symbol) {
        return ResponseEntity.ok(marketDataService.getQuote(symbol));
    }

    @PostMapping("/quotes")
    public ResponseEntity<List<StockQuoteDto>> getQuotes(@RequestBody List<String> symbols) {
        return ResponseEntity.ok(marketDataService.getQuotes(symbols));
    }

    @GetMapping("/candles/{symbol}")
    public ResponseEntity<List<CandleDto>> getCandles(
            @PathVariable String symbol,
            @RequestParam String resolution,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(marketDataService.getCandles(symbol, resolution, from, to));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Symbol>> searchSymbols(@RequestParam String query) {
        return ResponseEntity.ok(marketDataService.searchSymbols(query));
    }

    @GetMapping("/indices")
    public ResponseEntity<List<IndexQuoteDto>> getIndices() {
        return ResponseEntity.ok(marketDataService.getIndices());
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<Symbol> getSymbol(@PathVariable String symbol) {
        return ResponseEntity.ok(marketDataService.getSymbol(symbol));
    }

    @PostMapping("/sync-symbols-now")
    public ResponseEntity<String> syncSymbolsNow() {
        try {
            int count = symbolSyncJob.syncSymbolsNow();
            return ResponseEntity.ok("Successfully synced " + count + " symbols from NSE CSV.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error syncing symbols: " + e.getMessage());
        }
    }
}
