package in.sapphirus.rupee.portfolio.event;

import java.time.Instant;
import java.util.UUID;

public record PortfolioRecalcEvent(
        UUID userId,
        String symbol,
        UUID tradeId,
        String triggerReason,
        Instant timestamp
) {}
