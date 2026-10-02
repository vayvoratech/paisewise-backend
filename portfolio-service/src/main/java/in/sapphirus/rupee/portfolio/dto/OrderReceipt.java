package in.sapphirus.rupee.portfolio.dto;

import in.sapphirus.rupee.portfolio.domain.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderReceipt(
        UUID orderId,
        UUID userId,
        String clientOrderId,
        String symbol,
        String exchange,
        String side,
        String orderType,
        String product,
        int quantity,
        int filledQty,
        BigDecimal price,
        BigDecimal avgPrice,
        String status,
        String brokerOrderId,
        String message,
        Instant placedAt
) {
    public static OrderReceipt from(Order order, String message) {
        return new OrderReceipt(
                order.getId(),
                order.getUserId(),
                order.getClientOrderId(),
                order.getSymbol(),
                order.getExchange(),
                order.getSide(),
                order.getOrderType(),
                order.getProduct(),
                order.getQuantity(),
                order.getFilledQty(),
                order.getPrice(),
                order.getAvgPrice(),
                order.getStatus(),
                order.getBrokerOrderId(),
                message != null ? message : "Order processed successfully",
                order.getPlacedAt()
        );
    }
}
