package in.sapphirus.rupee.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.sapphirus.rupee.market.domain.Tick;
import in.sapphirus.rupee.market.dto.StockQuoteDto;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
public class TickIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TickIngestionService.class);

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fyers.websocket.url:ws://localhost:8087/mock/fyers}")
    private String wsUrl;

    @Value("${fyers.websocket.access-token:mock-token}")
    private String accessToken;

    private final Queue<Tick> buffer = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService simulatorService = Executors.newSingleThreadExecutor();
    private final ExecutorService kafkaExecutor = Executors.newSingleThreadExecutor();

    private WebSocketSession session;
    private int reconnectDelaySeconds = 1;
    private boolean isRunning = true;
    private boolean simulatorStarted = false;

    public TickIngestionService(JdbcTemplate jdbcTemplate, StringRedisTemplate redis,
                                KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.redis = redis;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        connectToWebSocket();
    }

    @PreDestroy
    public void cleanup() {
        this.isRunning = false;
        reconnectScheduler.shutdownNow();
        simulatorService.shutdownNow();
        kafkaExecutor.shutdownNow();
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.warn("Failed to close WebSocket session: {}", e.getMessage());
            }
        }
    }

    private void connectToWebSocket() {
        if (!isRunning) return;
        log.info("Attempting connection to Fyers WebSocket: {}", wsUrl);

        StandardWebSocketClient client = new StandardWebSocketClient();
        try {
            client.execute(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
                    session = wsSession;
                    reconnectDelaySeconds = 1; // reset backoff
                    log.info("Fyers WebSocket connection established successfully.");
                    
                    // Send authentication/subscription payload
                    Map<String, Object> req = Map.of(
                        "action", "subscribe",
                        "token", accessToken,
                        "symbols", List.of("NSE:RELIANCE", "NSE:TCS", "NSE:INFY", "NSE:HDFCBANK", "NSE:ICICIBANK")
                    );
                    wsSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(req)));
                }

                @Override
                protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
                    String payload = message.getPayload();
                    Tick tick = objectMapper.readValue(payload, Tick.class);
                    if (tick != null) {
                        ingestTick(tick);
                    }
                }

                @Override
                public void afterConnectionClosed(WebSocketSession wsSession, org.springframework.web.socket.CloseStatus status) throws Exception {
                    log.warn("Fyers WebSocket connection closed (Status: {}). Triggering reconnect...", status);
                    triggerReconnect();
                }

                @Override
                public void handleTransportError(WebSocketSession wsSession, Throwable exception) throws Exception {
                    log.error("Fyers WebSocket transport error: {}", exception.getMessage());
                }
            }, wsUrl).get(5, TimeUnit.SECONDS); // 5s timeout

        } catch (Exception e) {
            log.warn("Failed to connect to Fyers WebSocket broker ({}). Initializing simulated feed fallback.", e.getMessage());
            triggerReconnect();
            startSimulatorFeed();
        }
    }

    private void triggerReconnect() {
        if (!isRunning) return;
        log.info("Scheduling WebSocket reconnection in {} seconds (exponential backoff)...", reconnectDelaySeconds);
        reconnectScheduler.schedule(this::connectToWebSocket, reconnectDelaySeconds, TimeUnit.SECONDS);
        // Double delay up to 60s
        reconnectDelaySeconds = Math.min(reconnectDelaySeconds * 2, 60);
    }

    private void startSimulatorFeed() {
        if (simulatorStarted || !isRunning) return;
        simulatorStarted = true;
        log.info("Starting simulated ticks streaming loop.");
        
        simulatorService.submit(() -> {
            Random rand = new Random();
            Map<String, Double> basePrices = Map.of(
                "NSE:RELIANCE", 2952.0,
                "NSE:TCS", 3801.0,
                "NSE:INFY", 1456.0,
                "NSE:HDFCBANK", 1642.0,
                "NSE:ICICIBANK", 1104.0
            );

            while (isRunning) {
                try {
                    Thread.sleep(150); // Emit a mock tick packet every 150ms
                    String symbol = new ArrayList<>(basePrices.keySet()).get(rand.nextInt(basePrices.size()));
                    double base = basePrices.get(symbol);
                    double ltp = base + (rand.nextDouble() * 10 - 5); // fluctuate +/- 5
                    double open = base - 2;
                    double high = Math.max(ltp, base + 8);
                    double low = Math.min(ltp, base - 8);
                    double changePct = ((ltp - base) / base) * 100;
                    
                    Tick tick = new Tick();
                    tick.setTime(Instant.now());
                    tick.setSymbol(symbol);
                    tick.setLtp(ltp);
                    tick.setOpen(open);
                    tick.setHigh(high);
                    tick.setLow(low);
                    tick.setClose(ltp);
                    tick.setPrevClose(base);
                    tick.setVolume(100000L + rand.nextInt(50000));
                    tick.setChangePct(changePct);
                    
                    ingestTick(tick);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    log.error("Error in simulated tick ingestion: {}", e.getMessage());
                }
            }
        });
    }

    private void ingestTick(Tick tick) {
        // 1. Add to buffer for SQL batching
        buffer.add(tick);

        // 2. Update Redis Quote Cache (10s TTL)
        String quoteKey = "quote:" + tick.getSymbol();
        try {
            StockQuoteDto dto = new StockQuoteDto(
                tick.getSymbol(),
                getCompanyNameForSymbol(tick.getSymbol()),
                tick.getLtp(),
                tick.getChangePct(),
                List.of(tick.getLtp()), // sparkline updates
                getLogoForSymbol(tick.getSymbol())
            );
            redis.opsForValue().set(quoteKey, objectMapper.writeValueAsString(dto), Duration.ofSeconds(10));
        } catch (Exception e) {
            log.warn("Failed to update Redis quote cache for {}: {}", tick.getSymbol(), e.getMessage());
        }

        // 3. Kafka Broadcast (Asynchronous and non-blocking)
        kafkaExecutor.submit(() -> {
            try {
                kafkaTemplate.send("stock-ticks", tick.getSymbol(), objectMapper.writeValueAsString(tick));
            } catch (Exception e) {
                // Log warning and ignore to prevent WebSocket crash if Kafka broker is offline
                log.trace("Kafka broker offline. Skipping broadcast for symbol: {}", tick.getSymbol());
            }
        });
    }

    @Scheduled(fixedRate = 100)
    public void flushBuffer() {
        if (buffer.isEmpty()) return;

        List<Tick> batch = new ArrayList<>();
        Tick t;
        while ((t = buffer.poll()) != null) {
            batch.add(t);
        }

        if (batch.isEmpty()) return;

        String sql = "INSERT INTO ticks (time, symbol, ltp, open, high, low, close, prev_close, volume, change_pct) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (time, symbol) DO NOTHING";

        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Tick tick = batch.get(i);
                    ps.setTimestamp(1, Timestamp.from(tick.getTime()));
                    ps.setString(2, tick.getSymbol());
                    ps.setDouble(3, tick.getLtp());
                    ps.setDouble(4, tick.getOpen() != null ? tick.getOpen() : tick.getLtp());
                    ps.setDouble(5, tick.getHigh() != null ? tick.getHigh() : tick.getLtp());
                    ps.setDouble(6, tick.getLow() != null ? tick.getLow() : tick.getLtp());
                    ps.setDouble(7, tick.getClose() != null ? tick.getClose() : tick.getLtp());
                    ps.setDouble(8, tick.getPrevClose() != null ? tick.getPrevClose() : tick.getLtp());
                    ps.setLong(9, tick.getVolume() != null ? tick.getVolume() : 0L);
                    ps.setDouble(10, tick.getChangePct() != null ? tick.getChangePct() : 0.0);
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
            log.debug("Flushed batch of {} ticks to database ticks hypertable.", batch.size());
        } catch (Exception e) {
            log.error("Failed to execute ticks batch SQL ingestion: {}", e.getMessage());
        }
    }

    private String getCompanyNameForSymbol(String symbol) {
        switch (symbol) {
            case "NSE:RELIANCE": return "Reliance Industries Ltd.";
            case "NSE:TCS": return "Tata Consultancy Services Ltd.";
            case "NSE:INFY": return "Infosys Limited";
            case "NSE:HDFCBANK": return "HDFC Bank Limited";
            case "NSE:ICICIBANK": return "ICICI Bank Limited";
            default: return symbol;
        }
    }

    private String getLogoForSymbol(String symbol) {
        switch (symbol) {
            case "NSE:RELIANCE": return "🛢️";
            case "NSE:TCS": return "💻";
            case "NSE:INFY": return "🖥️";
            case "NSE:HDFCBANK": return "🏦";
            case "NSE:ICICIBANK": return "💳";
            default: return "📈";
        }
    }
}
