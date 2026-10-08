package in.sapphirus.rupee.portfolio.service;

import in.sapphirus.rupee.portfolio.client.FyersGateway;
import in.sapphirus.rupee.portfolio.domain.Order;
import in.sapphirus.rupee.portfolio.dto.FyersOrderRequest;
import in.sapphirus.rupee.portfolio.dto.FyersOrderResponse;
import in.sapphirus.rupee.portfolio.dto.OrderReceipt;
import in.sapphirus.rupee.portfolio.dto.PlaceOrderRequest;
import in.sapphirus.rupee.portfolio.event.OrderCreatedEvent;
import in.sapphirus.rupee.portfolio.event.PortfolioEventPublisher;
import in.sapphirus.rupee.portfolio.exception.KycNotVerifiedException;
import in.sapphirus.rupee.portfolio.repo.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealOrderServiceTest {

    @Mock
    private OrderRepository orderRepo;

    @Mock
    private RiskEngine riskEngine;

    @Mock
    private FyersGateway fyersGateway;

    @Mock
    private PortfolioEventPublisher eventPublisher;

    @InjectMocks
    private RealOrderService orderService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("placeOrder places real order successfully via FyersGateway and publishes orders.created")
    void placeOrder_success() {
        PlaceOrderRequest req = new PlaceOrderRequest(
                "TCS", "BUY", 5, "LIMIT", "CNC", "NSE",
                BigDecimal.valueOf(3500.0), null, "DAY", "CLIENT-101", "VERIFIED"
        );

        when(orderRepo.findWithPessimisticReadLockByClientOrderId("CLIENT-101")).thenReturn(Optional.empty());

        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) {
                o.setId(UUID.randomUUID());
            }
            return o;
        });

        FyersOrderResponse fyersResp = FyersOrderResponse.ok("FYERS-ORD-789", "Order placed");
        when(fyersGateway.placeOrder(any(FyersOrderRequest.class))).thenReturn(fyersResp);

        OrderReceipt receipt = orderService.placeOrder(userId, req);

        // Verify risk engine called
        verify(riskEngine).validateOrder(eq(userId), eq("TCS"), eq("BUY"), eq(5),
                eq(BigDecimal.valueOf(3500.0)), eq("VERIFIED"));

        // Verify FyersGateway called
        verify(fyersGateway).placeOrder(any(FyersOrderRequest.class));

        // Verify orders.created event published
        verify(eventPublisher).publishOrderCreated(any(OrderCreatedEvent.class));

        assertThat(receipt).isNotNull();
        assertThat(receipt.symbol()).isEqualTo("TCS");
        assertThat(receipt.side()).isEqualTo("BUY");
        assertThat(receipt.quantity()).isEqualTo(5);
        assertThat(receipt.status()).isEqualTo("OPEN");
        assertThat(receipt.brokerOrderId()).isEqualTo("FYERS-ORD-789");
    }

    @Test
    @DisplayName("placeOrder enforces idempotency via client_order_id without calling Fyers again")
    void placeOrder_idempotency_returnsExistingOrder() {
        PlaceOrderRequest req = new PlaceOrderRequest(
                "TCS", "BUY", 5, "LIMIT", "CNC", "NSE",
                BigDecimal.valueOf(3500.0), null, "DAY", "CLIENT-DUP-001", "VERIFIED"
        );

        Order existing = new Order(
                userId, "CLIENT-DUP-001", "TCS", "NSE", "BUY", "LIMIT", "CNC", 5,
                BigDecimal.valueOf(3500.0), null, "DAY", false
        );
        existing.setId(UUID.randomUUID());
        existing.setStatus("OPEN");
        existing.setBrokerOrderId("FYERS-EXISTING-123");

        when(orderRepo.findWithPessimisticReadLockByClientOrderId("CLIENT-DUP-001")).thenReturn(Optional.of(existing));

        OrderReceipt receipt = orderService.placeOrder(userId, req);

        // Should return existing order immediately
        assertThat(receipt).isNotNull();
        assertThat(receipt.brokerOrderId()).isEqualTo("FYERS-EXISTING-123");
        assertThat(receipt.message()).contains("Duplicate order skipped");

        // FyersGateway and RiskEngine should NOT be called for duplicate
        verifyNoInteractions(fyersGateway);
        verifyNoInteractions(riskEngine);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("placeOrder fails and propagates exception when KYC is not verified")
    void placeOrder_kycRejection_throwsException() {
        PlaceOrderRequest req = new PlaceOrderRequest(
                "TCS", "BUY", 5, "LIMIT", "CNC", "NSE",
                BigDecimal.valueOf(3500.0), null, "DAY", "CLIENT-KYC-FAIL", "PENDING"
        );

        when(orderRepo.findWithPessimisticReadLockByClientOrderId("CLIENT-KYC-FAIL")).thenReturn(Optional.empty());
        doThrow(new KycNotVerifiedException("KYC must be VERIFIED"))
                .when(riskEngine).validateOrder(any(), any(), any(), anyInt(), any(), any());

        assertThatThrownBy(() -> orderService.placeOrder(userId, req))
                .isInstanceOf(KycNotVerifiedException.class);

        verifyNoInteractions(fyersGateway);
    }

    @Test
    @DisplayName("getMyOrders returns list of orders for user")
    void getMyOrders_success() {
        Order order1 = new Order(userId, "C1", "INFY", "NSE", "BUY", "MARKET", "CNC", 10, null, null, "DAY", false);
        Order order2 = new Order(userId, "C2", "TCS", "NSE", "SELL", "LIMIT", "CNC", 5, BigDecimal.valueOf(3400.0), null, "DAY", false);

        when(orderRepo.findByUserIdOrderByPlacedAtDesc(userId)).thenReturn(List.of(order1, order2));

        List<Order> orders = orderService.getMyOrders(userId);
        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("cancelOrder cancels OPEN order with Fyers broker")
    void cancelOrder_success() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(userId, "C1", "INFY", "NSE", "BUY", "LIMIT", "CNC", 10, BigDecimal.valueOf(1500.0), null, "DAY", false);
        order.setId(orderId);
        order.setStatus("OPEN");
        order.setBrokerOrderId("FYERS-CANCEL-123");

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order cancelled = orderService.cancelOrder(userId, orderId);

        verify(fyersGateway).cancelOrder("FYERS-CANCEL-123");
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
    }
}
