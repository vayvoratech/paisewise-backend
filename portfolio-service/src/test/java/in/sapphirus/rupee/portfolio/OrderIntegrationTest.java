package in.sapphirus.rupee.portfolio;

import in.sapphirus.rupee.portfolio.client.FyersGateway;
import in.sapphirus.rupee.portfolio.domain.Holding;
import in.sapphirus.rupee.portfolio.domain.Ledger;
import in.sapphirus.rupee.portfolio.domain.Order;
import in.sapphirus.rupee.portfolio.domain.Trade;
import in.sapphirus.rupee.portfolio.repo.HoldingRepository;
import in.sapphirus.rupee.portfolio.repo.LedgerRepository;
import in.sapphirus.rupee.portfolio.repo.OrderRepository;
import in.sapphirus.rupee.portfolio.repo.TradeRepository;
import in.sapphirus.rupee.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private TradeRepository tradeRepo;

    @Autowired
    private LedgerRepository ledgerRepo;

    @Autowired
    private HoldingRepository holdingRepo;

    @Autowired
    private FyersGateway fyersGateway;

    private UUID userId;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jwtToken = "Bearer " + jwtService.issueAccessToken(userId.toString(), "+919876543210", java.util.Map.of());

        // Seed initial ledger balance of 100,000 for user
        Ledger initialDeposit = new Ledger(userId, "CREDIT", 100000.0, 100000.0, "Initial Deposit");
        ledgerRepo.save(initialDeposit);
    }

    @Test
    @DisplayName("End-to-End: Place real order -> Fyers webhook trade callback -> Ledger updated -> Holdings updated")
    void endToEnd_orderPlacement_and_webhookCallback() throws Exception {
        // ── 1. Place Real Order ───────────────────────────────────────────────
        String placeOrderJson = """
                {
                    "symbol": "INFY",
                    "side": "BUY",
                    "quantity": 10,
                    "orderType": "LIMIT",
                    "product": "CNC",
                    "price": 1500.00,
                    "clientOrderId": "CLI-E2E-001",
                    "kycStatus": "VERIFIED"
                }
                """;

        String placeOrderResp = mockMvc.perform(post("/portfolio/orders")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeOrderJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("INFY"))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.brokerOrderId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        List<Order> orders = orderRepo.findByUserIdOrderByPlacedAtDesc(userId);
        assertThat(orders).isNotEmpty();
        Order placedOrder = orders.get(0);
        String brokerOrderId = placedOrder.getBrokerOrderId();

        // ── 2. Query My Orders ────────────────────────────────────────────────
        mockMvc.perform(get("/portfolio/orders")
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("INFY"))
                .andExpect(jsonPath("$[0].clientOrderId").value("CLI-E2E-001"));

        // ── 3. Fyers Trade Execution Webhook Callback ─────────────────────────
        String webhookPayload = String.format("""
                {
                    "brokerTradeId": "TRD-E2E-%s",
                    "brokerOrderId": "%s",
                    "clientOrderId": "CLI-E2E-001",
                    "exchange": "NSE",
                    "fillQty": 10,
                    "fillPrice": 1500.00,
                    "brokerage": 20.0,
                    "stt": 15.0,
                    "gst": 3.6,
                    "sebiCharges": 0.15,
                    "stampDuty": 2.25,
                    "totalCharges": 41.0
                }
                """, System.currentTimeMillis(), brokerOrderId);

        String signature = fyersGateway.calculateHmacSha256(webhookPayload, fyersGateway.getProperties().getWebhookSecret());

        mockMvc.perform(post("/webhooks/fyers")
                        .header("X-Fyers-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        // ── 4. Verify Database State ──────────────────────────────────────────
        // Trade created
        List<Trade> trades = tradeRepo.findByUserIdOrderByTradedAtDesc(userId);
        assertThat(trades).isNotEmpty();
        Trade trade = trades.get(0);
        assertThat(trade.getSymbol()).isEqualTo("INFY");
        assertThat(trade.getFillQty()).isEqualTo(10);
        assertThat(trade.getNetAmount()).isEqualTo(15041.0); // 10*1500 + 41

        // Ledger debited
        Double remainingBalance = ledgerRepo.calculateBalance(userId);
        assertThat(remainingBalance).isEqualTo(100000.0 - 15041.0);

        // Holdings updated
        List<Holding> holdings = holdingRepo.findByUserId(userId);
        assertThat(holdings).isNotEmpty();
        Holding holding = holdings.stream().filter(h -> h.getSymbol().equals("INFY")).findFirst().orElseThrow();
        assertThat(holding.getQuantity()).isEqualTo(10);
        assertThat(holding.getTotalInvested()).isEqualTo(15041.0);

        // Order updated to COMPLETE
        Order updatedOrder = orderRepo.findById(placedOrder.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo("COMPLETE");
        assertThat(updatedOrder.getFilledQty()).isEqualTo(10);
    }

    @Test
    @DisplayName("Place real order fails with 403 Forbidden when KYC is not verified")
    void placeOrder_kycUnverified_rejected() throws Exception {
        String placeOrderJson = """
                {
                    "symbol": "TCS",
                    "side": "BUY",
                    "quantity": 5,
                    "orderType": "LIMIT",
                    "product": "CNC",
                    "price": 3500.00,
                    "clientOrderId": "CLI-KYC-FAIL-01",
                    "kycStatus": "PENDING"
                }
                """;

        mockMvc.perform(post("/portfolio/orders")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeOrderJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Place real order fails with 400 Bad Request when margin is insufficient")
    void placeOrder_insufficientMargin_rejected() throws Exception {
        UUID poorUserId = UUID.randomUUID();
        String poorUserToken = "Bearer " + jwtService.issueAccessToken(poorUserId.toString(), "+919876543211", java.util.Map.of());
        // Balance = 500
        ledgerRepo.save(new Ledger(poorUserId, "CREDIT", 500.0, 500.0, "Small Balance"));

        String placeOrderJson = """
                {
                    "symbol": "MRF",
                    "side": "BUY",
                    "quantity": 1,
                    "orderType": "LIMIT",
                    "product": "CNC",
                    "price": 120000.00,
                    "clientOrderId": "CLI-MARGIN-FAIL",
                    "kycStatus": "VERIFIED"
                }
                """;

        mockMvc.perform(post("/portfolio/orders")
                        .header("Authorization", poorUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeOrderJson))
                .andExpect(status().isBadRequest());
    }
}
