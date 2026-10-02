package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.domain.Order;
import in.sapphirus.rupee.portfolio.dto.OrderReceipt;
import in.sapphirus.rupee.portfolio.dto.PlaceOrderRequest;
import in.sapphirus.rupee.portfolio.service.RealOrderService;
import in.sapphirus.rupee.portfolio.service.RiskEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private RealOrderService orderService;

    @Mock
    private RiskEngine riskEngine;

    @InjectMocks
    private OrderController orderController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("placeOrder calls orderService and returns receipt")
    void placeOrder_success() {
        PlaceOrderRequest req = new PlaceOrderRequest(
                "RELIANCE", "BUY", 10, "LIMIT", "CNC", "NSE",
                BigDecimal.valueOf(2500.0), null, "DAY", "CLI-001", "VERIFIED"
        );

        OrderReceipt receipt = new OrderReceipt(
                UUID.randomUUID(), userId, "CLI-001", "RELIANCE", "NSE", "BUY",
                "LIMIT", "CNC", 10, 0, BigDecimal.valueOf(2500.0), null, "OPEN",
                "FYERS-100", "Order placed", Instant.now()
        );

        when(orderService.placeOrder(eq(userId), any(PlaceOrderRequest.class))).thenReturn(receipt);

        OrderReceipt result = orderController.placeOrder(req);
        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("RELIANCE");
        assertThat(result.status()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("myOrders returns list of user orders")
    void myOrders_success() {
        Order order = new Order(userId, "CLI-1", "INFY", "NSE", "BUY", "MARKET", "CNC", 5, null, null, "DAY", false);
        order.setId(UUID.randomUUID());
        when(orderService.getMyOrders(userId)).thenReturn(List.of(order));

        List<OrderReceipt> list = orderController.myOrders();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).symbol()).isEqualTo("INFY");
    }

    @Test
    @DisplayName("riskStatus returns current ledger balance and risk limits")
    void getRiskStatus_success() {
        when(riskEngine.getAvailableLedgerBalance(userId)).thenReturn(BigDecimal.valueOf(25000.0));

        Map<String, Object> status = orderController.getRiskStatus();
        assertThat(status.get("availableLedgerBalance")).isEqualTo(BigDecimal.valueOf(25000.0));
        assertThat(status.get("maxOpenPositions")).isEqualTo(50);
        assertThat(status.get("maxOrdersPerMinute")).isEqualTo(10);
        assertThat(status.get("requiredKycStatus")).isEqualTo("VERIFIED");
    }
}
