package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.domain.Order;
import in.sapphirus.rupee.portfolio.dto.OrderReceipt;
import in.sapphirus.rupee.portfolio.dto.PlaceOrderRequest;
import in.sapphirus.rupee.portfolio.service.RealOrderService;
import in.sapphirus.rupee.portfolio.service.RiskEngine;
import in.sapphirus.rupee.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for Real Trading Orders (/portfolio/orders).
 */
@RestController
@RequestMapping({"/portfolio/orders", "/orders"})
public class OrderController {

    private final RealOrderService orderService;
    private final RiskEngine riskEngine;

    public OrderController(RealOrderService orderService, RiskEngine riskEngine) {
        this.orderService = orderService;
        this.riskEngine = riskEngine;
    }

    /**
     * Place a real order.
     * Enforces KYC verification, RiskEngine checks, Fyers API placement, and orders.created event.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderReceipt placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return orderService.placeOrder(userId, request);
    }

    /**
     * List all orders placed by the current authenticated user.
     */
    @GetMapping
    public List<OrderReceipt> myOrders() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return orderService.getMyOrders(userId).stream()
                .map(o -> OrderReceipt.from(o, null))
                .toList();
    }

    /**
     * Get a specific order by ID.
     */
    @GetMapping("/{orderId}")
    public OrderReceipt getOrder(@PathVariable UUID orderId) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        Order order = orderService.getOrder(userId, orderId);
        return OrderReceipt.from(order, null);
    }

    /**
     * Cancel an active real order.
     */
    @DeleteMapping("/{orderId}")
    public OrderReceipt cancelOrder(@PathVariable UUID orderId) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        Order order = orderService.cancelOrder(userId, orderId);
        return OrderReceipt.from(order, "Order cancelled successfully");
    }

    /**
     * Margin & Risk status preview helper endpoint.
     */
    @GetMapping("/risk-status")
    public Map<String, Object> getRiskStatus() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        BigDecimal balance = riskEngine.getAvailableLedgerBalance(userId);
        return Map.of(
                "userId", userId,
                "availableLedgerBalance", balance,
                "maxOpenPositions", RiskEngine.MAX_OPEN_POSITIONS,
                "maxOrdersPerMinute", RiskEngine.MAX_ORDERS_PER_MINUTE,
                "requiredKycStatus", RiskEngine.REQUIRED_KYC_STATUS
        );
    }
}
