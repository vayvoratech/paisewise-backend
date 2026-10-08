package in.sapphirus.rupee.practice.api;

import in.sapphirus.rupee.practice.domain.Order;
import in.sapphirus.rupee.practice.domain.PaperAccount;
import in.sapphirus.rupee.practice.domain.PaperPosition;
import in.sapphirus.rupee.practice.domain.Stock;
import in.sapphirus.rupee.practice.repo.OrderRepository;
import in.sapphirus.rupee.practice.repo.PaperAccountRepository;
import in.sapphirus.rupee.practice.repo.PaperPositionRepository;
import in.sapphirus.rupee.practice.repo.StockRepository;
import in.sapphirus.rupee.practice.service.PaperOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PracticeControllerTest {

    private final StockRepository stockRepository = mock(StockRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final PaperOrderService paperOrderService = mock(PaperOrderService.class);
    private final PaperAccountRepository paperAccountRepository = mock(PaperAccountRepository.class);
    private final PaperPositionRepository paperPositionRepository = mock(PaperPositionRepository.class);

    private PracticeController controller;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reset(stockRepository, orderRepository, paperOrderService, paperAccountRepository, paperPositionRepository);
        controller = new PracticeController(
                stockRepository,
                orderRepository,
                paperOrderService,
                paperAccountRepository,
                paperPositionRepository
        );

        // Set up mock security context with authenticated userId
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void account_returnsAccountViewWithPositions() {
        PaperAccount account = new PaperAccount(userId);
        account.setBalance(new BigDecimal("95000.00"));
        account.setReservedBalance(new BigDecimal("5000.00"));
        when(paperOrderService.getOrCreateAccount(userId)).thenReturn(account);

        PaperPosition pos = new PaperPosition(userId, "RELIANCE");
        pos.setQuantity(10);
        pos.setReservedQuantity(2);
        when(paperPositionRepository.findByUserId(userId)).thenReturn(List.of(pos));

        PracticeController.AccountView view = controller.account();

        assertThat(view).isNotNull();
        assertThat(view.balance()).isEqualByComparingTo("95000.00");
        assertThat(view.reservedBalance()).isEqualByComparingTo("5000.00");
        assertThat(view.positions()).hasSize(1);
        assertThat(view.positions().get(0).symbol()).isEqualTo("RELIANCE");
        assertThat(view.positions().get(0).quantity()).isEqualTo(10);
        assertThat(view.positions().get(0).reservedQuantity()).isEqualTo(2);
    }

    @Test
    void resetAccount_callsServiceAndReturnsUpdatedView() {
        PaperAccount resetAccount = new PaperAccount(userId);
        resetAccount.setBalance(new BigDecimal("100000.00"));
        resetAccount.setReservedBalance(BigDecimal.ZERO);
        when(paperOrderService.resetAccount(userId)).thenReturn(resetAccount);
        when(paperPositionRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        PracticeController.AccountView view = controller.resetAccount();

        assertThat(view).isNotNull();
        assertThat(view.balance()).isEqualByComparingTo("100000.00");
        assertThat(view.reservedBalance()).isEqualByComparingTo("0.00");
        assertThat(view.positions()).isEmpty();
        verify(paperOrderService).resetAccount(userId);
    }

    @Test
    void placeOrder_marketBuy_success() {
        Stock stock = new Stock("RELIANCE", "Reliance", 2500.0, 1.0, "🛢️", "[]");
        when(stockRepository.findById("RELIANCE")).thenReturn(Optional.of(stock));

        Order order = new Order(userId, "cid-1", "RELIANCE", "NSE", "BUY", "MARKET", "CNC", 5, true);
        order.setStatus("COMPLETE");
        order.setPrice(new BigDecimal("2500.00"));
        when(paperOrderService.executeMarketBuy(userId, stock, 5, "client-market-buy")).thenReturn(order);

        PracticeController.PlaceOrderRequest req = new PracticeController.PlaceOrderRequest(
                "RELIANCE", "BUY", 5, "MARKET", null, "client-market-buy"
        );

        PracticeController.OrderReceipt receipt = controller.placeOrder(req);

        assertThat(receipt).isNotNull();
        assertThat(receipt.symbol()).isEqualTo("RELIANCE");
        assertThat(receipt.side()).isEqualTo("BUY");
        assertThat(receipt.shares()).isEqualTo(5);
        assertThat(receipt.pricePerShare()).isEqualByComparingTo("2500.00");
        assertThat(receipt.totalPaid()).isEqualByComparingTo("12500.00");
        assertThat(receipt.status()).isEqualTo("COMPLETE");
    }

    @Test
    void placeOrder_limitBuy_requiresPrice() {
        Stock stock = new Stock("RELIANCE", "Reliance", 2500.0, 1.0, "🛢️", "[]");
        when(stockRepository.findById("RELIANCE")).thenReturn(Optional.of(stock));

        PracticeController.PlaceOrderRequest req = new PracticeController.PlaceOrderRequest(
                "RELIANCE", "BUY", 5, "LIMIT", null, "client-limit-buy"
        );

        assertThatThrownBy(() -> controller.placeOrder(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Price is required for LIMIT orders");
    }

    @Test
    void placeOrder_limitBuy_success() {
        Stock stock = new Stock("RELIANCE", "Reliance", 2500.0, 1.0, "🛢️", "[]");
        when(stockRepository.findById("RELIANCE")).thenReturn(Optional.of(stock));

        BigDecimal limitPrice = new BigDecimal("2400.00");
        Order order = new Order(userId, "cid-limit", "RELIANCE", "NSE", "BUY", "LIMIT", "CNC", 5, true);
        order.setStatus("OPEN");
        order.setPrice(limitPrice);
        when(paperOrderService.placeLimitBuy(userId, stock, 5, limitPrice, "client-limit-buy")).thenReturn(order);

        PracticeController.PlaceOrderRequest req = new PracticeController.PlaceOrderRequest(
                "RELIANCE", "BUY", 5, "LIMIT", limitPrice, "client-limit-buy"
        );

        PracticeController.OrderReceipt receipt = controller.placeOrder(req);

        assertThat(receipt).isNotNull();
        assertThat(receipt.status()).isEqualTo("OPEN");
        assertThat(receipt.pricePerShare()).isEqualByComparingTo(limitPrice);
        assertThat(receipt.totalPaid()).isEqualByComparingTo("12000.00");
    }

    @Test
    void myOrders_returnsUserOrders() {
        Order order = new Order(userId, "cid", "RELIANCE", "NSE", "BUY", "MARKET", "CNC", 10, true);
        order.setPrice(new BigDecimal("2500.00"));
        order.setStatus("COMPLETE");
        when(orderRepository.findByUserIdOrderByPlacedAtDesc(userId)).thenReturn(List.of(order));

        List<PracticeController.OrderReceipt> receipts = controller.myOrders();

        assertThat(receipts).hasSize(1);
        assertThat(receipts.get(0).symbol()).isEqualTo("RELIANCE");
    }

    @Test
    void cancelOrder_delegatesToService() {
        UUID orderId = UUID.randomUUID();
        controller.cancelOrder(orderId);
        verify(paperOrderService).cancelOrder(userId, orderId);
    }
}
