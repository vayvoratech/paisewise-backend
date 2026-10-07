package in.sapphirus.rupee.portfolio.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FyersTradeWebhookPayload(
        String tradeId,
        String brokerTradeId,
        String orderId,
        String brokerOrderId,
        String clientOrderId,
        UUID userId,
        String symbol,
        String exchange,
        String side,            // BUY | SELL
        int fillQty,
        BigDecimal fillPrice,
        BigDecimal brokerage,
        BigDecimal stt,
        BigDecimal gst,
        BigDecimal sebiCharges,
        BigDecimal stampDuty,
        BigDecimal totalCharges,
        BigDecimal netAmount,
        String tradeTime
) {}
