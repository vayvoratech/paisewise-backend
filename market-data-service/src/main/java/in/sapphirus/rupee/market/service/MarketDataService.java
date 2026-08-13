package in.sapphirus.rupee.market.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.sapphirus.rupee.market.domain.Symbol;
import in.sapphirus.rupee.market.domain.Tick;
import in.sapphirus.rupee.market.dto.CandleDto;
import in.sapphirus.rupee.market.dto.IndexQuoteDto;
import in.sapphirus.rupee.market.dto.StockQuoteDto;
import in.sapphirus.rupee.market.repo.SymbolRepository;
import in.sapphirus.rupee.market.repo.TickRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MarketDataService {

    private final SymbolRepository symbolRepository;
    private final TickRepository tickRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MarketDataService(SymbolRepository symbolRepository,
                             TickRepository tickRepository,
                             StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper) {
        this.symbolRepository = symbolRepository;
        this.tickRepository = tickRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public StockQuoteDto getQuote(String symbol) {
        String key = "quote:" + symbol;
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, StockQuoteDto.class);
            }
        } catch (Exception e) {
            // Log and continue on Redis failure
            System.err.println("Redis cache read failure: " + e.getMessage());
        }

        // DB Fallback
        Optional<Symbol> symOpt = symbolRepository.findById(symbol);
        if (symOpt.isEmpty() && !symbol.contains(":")) {
            symOpt = symbolRepository.findById("NSE:" + symbol);
        }
        Symbol sym = symOpt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Symbol not found: " + symbol));
        String resolvedSymbol = sym.getSymbol();

        Optional<Tick> latestTickOpt = tickRepository.findLatestTick(resolvedSymbol);
        
        StockQuoteDto quote;
        if (latestTickOpt.isPresent()) {
            Tick tick = latestTickOpt.get();
            List<Double> trend = tickRepository.findRecentTrend(resolvedSymbol, 10);
            if (trend.isEmpty()) {
                trend = List.of(tick.getLtp());
            }
            quote = new StockQuoteDto(
                    sym.getSymbol(),
                    sym.getCompanyName(),
                    tick.getLtp(),
                    tick.getChangePct() != null ? tick.getChangePct() : 0.0,
                    trend,
                    sym.getLogoUrl() != null && !sym.getLogoUrl().isBlank() ? sym.getLogoUrl() : "📈"
            );
        } else {
            // Default quote using base symbol details if no ticks exist yet
            quote = new StockQuoteDto(
                    sym.getSymbol(),
                    sym.getCompanyName(),
                    100.0, // Default price
                    0.0,
                    List.of(100.0),
                    sym.getLogoUrl() != null && !sym.getLogoUrl().isBlank() ? sym.getLogoUrl() : "📈"
            );
        }

        // Cache in Redis for 10 seconds
        try {
            String json = objectMapper.writeValueAsString(quote);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(10));
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize quote to JSON: " + e.getMessage());
        }

        return quote;
    }

    public List<StockQuoteDto> getQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }

        // Cap batch requests at 50 max to ensure db scaling stability
        List<String> capped = symbols.stream().limit(50).collect(Collectors.toList());
        List<StockQuoteDto> result = new ArrayList<>();

        for (String symbol : capped) {
            try {
                result.add(getQuote(symbol));
            } catch (Exception e) {
                // Ignore missing symbols during batch fetch
                System.err.println("Failed to fetch quote for " + symbol + ": " + e.getMessage());
            }
        }
        return result;
    }

    public List<CandleDto> getCandles(String symbol, String resolution, Instant from, Instant to) {
        // Enforce basic validation
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing from/to date parameters");
        }

        String resolvedSymbol = symbol;
        if (!symbol.contains(":")) {
            Optional<Symbol> symOpt = symbolRepository.findById("NSE:" + symbol);
            if (symOpt.isPresent()) {
                resolvedSymbol = symOpt.get().getSymbol();
            }
        }

        List<Object[]> rawCandles;
        String res = resolution.toLowerCase();

        switch (res) {
            case "1m":
            case "1min":
            case "1minute":
                rawCandles = tickRepository.getCandles1m(resolvedSymbol, from, to);
                break;
            case "5m":
            case "5min":
            case "5minutes":
                rawCandles = tickRepository.getCandles5m(resolvedSymbol, from, to);
                break;
            case "15m":
            case "15min":
            case "15minutes":
                rawCandles = tickRepository.getCandles15m(resolvedSymbol, from, to);
                break;
            case "1h":
            case "1hr":
            case "1hour":
                rawCandles = tickRepository.getCandles1h(resolvedSymbol, from, to);
                break;
            case "1d":
            case "1day":
            case "daily":
                rawCandles = tickRepository.getCandles1d(resolvedSymbol, from, to);
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported resolution timeframe: " + resolution);
        }

        return rawCandles.stream().map(row -> {
            Instant bucket;
            if (row[0] instanceof Timestamp) {
                bucket = ((Timestamp) row[0]).toInstant();
            } else if (row[0] instanceof Instant) {
                bucket = (Instant) row[0];
            } else {
                bucket = Instant.parse(row[0].toString());
            }

            return new CandleDto(
                    bucket,
                    row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                    row[2] != null ? ((Number) row[2]).doubleValue() : 0.0,
                    row[3] != null ? ((Number) row[3]).doubleValue() : 0.0,
                    row[4] != null ? ((Number) row[4]).doubleValue() : 0.0,
                    row[5] != null ? ((Number) row[5]).longValue() : 0L
            );
        }).collect(Collectors.toList());
    }

    public List<Symbol> searchSymbols(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        String queryEscaped = "%" + query.trim() + "%";
        return symbolRepository.searchFuzzy(query.trim(), queryEscaped, 10);
    }

    public List<IndexQuoteDto> getIndices() {
        List<IndexQuoteDto> indices = new ArrayList<>();

        // Fetch index levels from ticks if they exist
        String[] indexSymbols = new String[]{"NSE:NIFTY50", "BSE:SENSEX", "NSE:NIFTYBANK"};
        for (String indexSymbol : indexSymbols) {
            Optional<Tick> tickOpt = tickRepository.findLatestTick(indexSymbol);
            if (tickOpt.isPresent()) {
                Tick tick = tickOpt.get();
                indices.add(new IndexQuoteDto(
                        indexSymbol,
                        tick.getLtp(),
                        tick.getChangePct() != null ? tick.getChangePct() : 0.0
                ));
            }
        }

        // Fallback to static mock index quotes if no ticks have been generated
        if (indices.isEmpty()) {
            indices.add(new IndexQuoteDto("NIFTY 50", 22456.0, 0.5));
            indices.add(new IndexQuoteDto("SENSEX", 73500.0, 0.35));
            indices.add(new IndexQuoteDto("NIFTY BANK", 48120.0, -0.15));
        }

        return indices;
    }
}
