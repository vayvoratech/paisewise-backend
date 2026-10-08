package in.sapphirus.rupee.practice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.sapphirus.rupee.practice.domain.PriceAlert;
import in.sapphirus.rupee.practice.domain.Stock;
import in.sapphirus.rupee.practice.dto.CreateAlertRequest;
import in.sapphirus.rupee.practice.dto.Tick;
import in.sapphirus.rupee.practice.repo.PriceAlertRepository;
import in.sapphirus.rupee.practice.repo.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PriceAlertService {

    private static final Logger log = LoggerFactory.getLogger(PriceAlertService.class);

    private final PriceAlertRepository repository;
    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper;

    public PriceAlertService(PriceAlertRepository repository,
                             StockRepository stockRepository,
                             ObjectMapper objectMapper) {
        this.repository = repository;
        this.stockRepository = stockRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PriceAlert createAlert(UUID userId, CreateAlertRequest request) {
        String symbol = request.symbol();
        String lookupSymbol = symbol.contains(":") ? symbol.substring(symbol.indexOf(":") + 1) : symbol;
        
        Stock stock = stockRepository.findById(lookupSymbol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found: " + symbol));

        BigDecimal currentLtp = BigDecimal.valueOf(stock.getPrice());
        BigDecimal target = request.targetPrice();
        String cond = request.condition().toUpperCase();

        // Enforce chk_target_price_makes_sense constraint before database write
        if ("GT".equals(cond) && target.compareTo(currentLtp) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target price must be greater than current price (" + currentLtp + ") for GT condition");
        }
        if ("GTE".equals(cond) && target.compareTo(currentLtp) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target price must be >= current price (" + currentLtp + ") for GTE condition");
        }
        if ("LT".equals(cond) && target.compareTo(currentLtp) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target price must be less than current price (" + currentLtp + ") for LT condition");
        }
        if ("LTE".equals(cond) && target.compareTo(currentLtp) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target price must be <= current price (" + currentLtp + ") for LTE condition");
        }

        PriceAlert alert = new PriceAlert(
                userId,
                symbol,
                cond,
                target,
                currentLtp,
                request.note(),
                request.expiresAt()
        );

        return repository.save(alert);
    }

    public List<PriceAlert> getUserAlerts(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void deleteAlert(UUID userId, UUID id) {
        PriceAlert alert = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        
        if (!alert.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        alert.setStatus("CANCELLED");
        alert.setUpdatedAt(Instant.now());
        repository.save(alert);
    }

    @KafkaListener(topics = "stock-ticks", groupId = "practice-alerts-group")
    public void processAlerts(String message) {
        try {
            Tick tick = objectMapper.readValue(message, Tick.class);
            String tickSymbol = tick.symbol();
            if (tickSymbol == null || tick.ltp() == null) return;

            // Fetch alerts directly matching symbol
            List<PriceAlert> activeAlerts = new ArrayList<>(repository.findActiveAlertsForSymbol(tickSymbol));

            // Also match without prefix (e.g. if ticks contain 'NSE:RELIANCE' but alert is saved as 'RELIANCE')
            if (tickSymbol.contains(":")) {
                String strippedSymbol = tickSymbol.substring(tickSymbol.indexOf(":") + 1);
                List<PriceAlert> strippedAlerts = repository.findActiveAlertsForSymbol(strippedSymbol);
                for (PriceAlert a : strippedAlerts) {
                    if (activeAlerts.stream().noneMatch(exist -> exist.getId().equals(a.getId()))) {
                        activeAlerts.add(a);
                    }
                }
            }

            for (PriceAlert alert : activeAlerts) {
                if (alert.getExpiresAt() != null && alert.getExpiresAt().isBefore(Instant.now())) {
                    alert.setStatus("EXPIRED");
                    alert.setUpdatedAt(Instant.now());
                    repository.save(alert);
                    continue;
                }
                if (shouldTrigger(alert, tick.ltp())) {
                    triggerAlert(alert, tick.ltp());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process price alert tick stream", e);
        }
    }

    private boolean shouldTrigger(PriceAlert alert, double currentPrice) {
        double target = alert.getTargetPrice().doubleValue();
        return switch (alert.getCondition().toUpperCase()) {
            case "GT" -> currentPrice > target;
            case "GTE" -> currentPrice >= target;
            case "LT" -> currentPrice < target;
            case "LTE" -> currentPrice <= target;
            default -> false;
        };
    }

    @Transactional
    public void triggerAlert(PriceAlert alert, double triggerPrice) {
        if ("ACTIVE".equalsIgnoreCase(alert.getStatus())) {
            alert.setStatus("TRIGGERED");
            alert.setTriggeredPrice(BigDecimal.valueOf(triggerPrice));
            alert.setTriggeredAt(Instant.now());
            alert.setUpdatedAt(Instant.now());
            repository.save(alert);
            log.info("Price alert triggered: alert_id={}, symbol={}, triggerPrice={}", alert.getId(), alert.getSymbol(), triggerPrice);
            dispatchNotification(alert, triggerPrice);
        }
    }

    private void dispatchNotification(PriceAlert alert, double triggerPrice) {
        log.info("DISPATCH NOTIFICATION: Alert for user {} on symbol {} triggered at price {}", 
                 alert.getUserId(), alert.getSymbol(), triggerPrice);
    }
}
