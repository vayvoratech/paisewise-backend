package in.sapphirus.rupee.portfolio.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID userId,
        String clientOrderId,
        String symbol,
        String exchange,
        String side,
        String orderType,
        String product,
        int quantity,
        BigDecimal price,
        String brokerOrderId,
        String status,
        Instant timestamp
) {}
