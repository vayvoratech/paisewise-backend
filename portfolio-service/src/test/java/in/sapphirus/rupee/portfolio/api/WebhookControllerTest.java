package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.client.FyersGateway;
import in.sapphirus.rupee.portfolio.domain.Holding;
import in.sapphirus.rupee.portfolio.domain.Ledger;
import in.sapphirus.rupee.portfolio.domain.Order;
import in.sapphirus.rupee.portfolio.domain.Trade;
import in.sapphirus.rupee.portfolio.event.PortfolioEventPublisher;
import in.sapphirus.rupee.portfolio.event.PortfolioRecalcEvent;
import in.sapphirus.rupee.portfolio.repo.HoldingRepository;
import in.sapphirus.rupee.portfolio.repo.LedgerRepository;
import in.sapphirus.rupee.portfolio.repo.OrderRepository;
import in.sapphirus.rupee.portfolio.repo.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock
    private FyersGateway fyersGateway;

    @Mock
    private TradeRepository tradeRepo;

    @Mock
    private LedgerRepository ledgerRepo;

    @Mock
    private HoldingRepository holdingRepo;

    @Mock
    private OrderRepository orderRepo;

    @Mock
    private PortfolioEventPublisher eventPublisher;

    @InjectMocks
    private WebhookController webhookController;

    private UUID userId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("handleFyersWebhook rejects request with invalid HMAC signature")
    void handleFyersWebhook_invalidSignature_badRequest() {
        String body = "{\"tradeId\":\"T1\"}";
        when(fyersGateway.verifyWebhookSignature(body, "bad_sig")).thenReturn(false);

        ResponseEntity<String> response = webhookController.handleFyersWebhook(body, "bad_sig", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Invalid webhook signature");
        verifyNoInteractions(tradeRepo);
        verifyNoInteractions(ledgerRepo);
    }

    @Test
    @DisplayName("handleFyersWebhook successfully processes BUY trade execution, debits ledger, updates holding, publishes portfolio.recalc")
    void handleFyersWebhook_buyTrade_success() {
        String body = """
                {
                    "brokerTradeId": "TRD-FYERS-001",
                    "brokerOrderId": "ORD-FYERS-001",
                    "exchange": "NSE",
                    "fillQty": 10,
                    "fillPrice": 1500.0,
                    "brokerage": 20.0,
                    "stt": 15.0,
                    "gst": 3.6,
                    "sebiCharges": 0.15,
                    "stampDuty": 2.25,
                    "totalCharges": 41.0
                }
                """;

        when(fyersGateway.verifyWebhookSignature(body, "valid_sig")).thenReturn(true);
        when(tradeRepo.existsByBrokerTradeId("TRD-FYERS-001")).thenReturn(false);

        Order existingOrder = new Order(
                userId, "CLIENT-001", "INFY", "NSE", "BUY", "LIMIT", "CNC", 10,
                BigDecimal.valueOf(1500.0), null, "DAY", false
        );
        existingOrder.setId(orderId);
        existingOrder.setBrokerOrderId("ORD-FYERS-001");
        existingOrder.setStatus("OPEN");

        when(orderRepo.findByBrokerOrderId("ORD-FYERS-001")).thenReturn(Optional.of(existingOrder));
        when(tradeRepo.save(any(Trade.class))).thenAnswer(i -> {
            Trade t = i.getArgument(0);
            return t;
        });

        when(ledgerRepo.calculateBalance(userId)).thenReturn(50000.0);
        when(holdingRepo.findByUserIdAndSymbolAndProductAndIsPaper(userId, "INFY", "CNC", false))
                .thenReturn(Optional.empty());

        ResponseEntity<String> response = webhookController.handleFyersWebhook(body, "valid_sig", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"ok\"");

        // 1. Verify Trade Log Inserted
        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo).save(tradeCaptor.capture());
        Trade trade = tradeCaptor.getValue();
        assertThat(trade.getSymbol()).isEqualTo("INFY");
        assertThat(trade.getFillQty()).isEqualTo(10);
        assertThat(trade.getFillPrice()).isEqualTo(1500.0);
        assertThat(trade.getTotalCharges()).isEqualTo(41.0);
        assertThat(trade.getNetAmount()).isEqualTo(15041.0); // 10*1500 + 41

        // 2. Verify Ledger Debited
        ArgumentCaptor<Ledger> ledgerCaptor = ArgumentCaptor.forClass(Ledger.class);
        verify(ledgerRepo).save(ledgerCaptor.capture());
        Ledger ledger = ledgerCaptor.getValue();
        assertThat(ledger.getUserId()).isEqualTo(userId);
        assertThat(ledger.getType()).isEqualTo("DEBIT");
        assertThat(ledger.getAmount()).isEqualTo(15041.0);
        assertThat(ledger.getBalanceAfter()).isEqualTo(34959.0); // 50000 - 15041

        // 3. Verify Holding Created
        ArgumentCaptor<Holding> holdingCaptor = ArgumentCaptor.forClass(Holding.class);
        verify(holdingRepo).save(holdingCaptor.capture());
        Holding holding = holdingCaptor.getValue();
        assertThat(holding.getSymbol()).isEqualTo("INFY");
        assertThat(holding.getQuantity()).isEqualTo(10);
        assertThat(holding.getTotalInvested()).isEqualTo(15041.0);

        // 4. Verify Order updated to COMPLETE
        verify(orderRepo).save(existingOrder);
        assertThat(existingOrder.getStatus()).isEqualTo("COMPLETE");
        assertThat(existingOrder.getFilledQty()).isEqualTo(10);

        // 5. Verify portfolio.recalc published
        verify(eventPublisher).publishPortfolioRecalc(any(PortfolioRecalcEvent.class));
    }

    @Test
    @DisplayName("handleFyersWebhook successfully processes SELL trade execution and credits ledger")
    void handleFyersWebhook_sellTrade_creditsLedger() {
        String body = """
                {
                    "brokerTradeId": "TRD-FYERS-002",
                    "brokerOrderId": "ORD-FYERS-002",
                    "exchange": "NSE",
                    "fillQty": 5,
                    "fillPrice": 2000.0,
                    "totalCharges": 20.0
                }
                """;

        when(fyersGateway.verifyWebhookSignature(body, "valid_sig")).thenReturn(true);
        when(tradeRepo.existsByBrokerTradeId("TRD-FYERS-002")).thenReturn(false);

        Order sellOrder = new Order(
                userId, "CLIENT-002", "TCS", "NSE", "SELL", "LIMIT", "CNC", 5,
                BigDecimal.valueOf(2000.0), null, "DAY", false
        );
        sellOrder.setId(orderId);
        sellOrder.setBrokerOrderId("ORD-FYERS-002");

        when(orderRepo.findByBrokerOrderId("ORD-FYERS-002")).thenReturn(Optional.of(sellOrder));
        when(tradeRepo.save(any(Trade.class))).thenAnswer(i -> i.getArgument(0));
        when(ledgerRepo.calculateBalance(userId)).thenReturn(10000.0);

        Holding holding = new Holding(userId, "TCS", 10, 1800.0, 18000.0, "CNC", false);
        when(holdingRepo.findByUserIdAndSymbolAndProductAndIsPaper(userId, "TCS", "CNC", false))
                .thenReturn(Optional.of(holding));

        ResponseEntity<String> response = webhookController.handleFyersWebhook(body, "valid_sig", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify Ledger Credited: netAmount = 5*2000 - 20 = 9980
        ArgumentCaptor<Ledger> ledgerCaptor = ArgumentCaptor.forClass(Ledger.class);
        verify(ledgerRepo).save(ledgerCaptor.capture());
        Ledger ledger = ledgerCaptor.getValue();
        assertThat(ledger.getType()).isEqualTo("CREDIT");
        assertThat(ledger.getAmount()).isEqualTo(9980.0);
        assertThat(ledger.getBalanceAfter()).isEqualTo(19980.0); // 10000 + 9980

        // Holding reduced from 10 to 5
        assertThat(holding.getQuantity()).isEqualTo(5);

        // portfolio.recalc event published
        verify(eventPublisher).publishPortfolioRecalc(any(PortfolioRecalcEvent.class));
    }

    @Test
    @DisplayName("handleFyersWebhook skips already processed brokerTradeId (Idempotency)")
    void handleFyersWebhook_duplicateTrade_skipped() {
        String body = "{\"brokerTradeId\":\"TRD-DUP-001\"}";
        when(fyersGateway.verifyWebhookSignature(body, "valid_sig")).thenReturn(true);
        when(tradeRepo.existsByBrokerTradeId("TRD-DUP-001")).thenReturn(true);

        ResponseEntity<String> response = webhookController.handleFyersWebhook(body, "valid_sig", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("already_processed");
        verify(tradeRepo, never()).save(any());
        verify(ledgerRepo, never()).save(any());
    }
}
