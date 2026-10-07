package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.service.AiFundRecommendationService;
import in.sapphirus.rupee.portfolio.service.AmfiNavSyncService;
import in.sapphirus.rupee.portfolio.service.BseMfOrderService;
import in.sapphirus.rupee.portfolio.service.FundService;
import in.sapphirus.rupee.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("FundController Web API Tests")
class FundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FundService fundService;

    @MockBean
    private BseMfOrderService bseOrderService;

    @MockBean
    private AmfiNavSyncService amfiSyncService;

    @Autowired
    private JwtService jwtService;

    private String jwtToken;
    private final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        jwtToken = jwtService.issueAccessToken(USER_ID.toString(), "+919876543210", Map.of());
    }

    @Test
    @DisplayName("GET /portfolio/funds/categories returns category list publicly")
    void getCategories_public() throws Exception {
        when(fundService.getCategories()).thenReturn(List.of("Large Cap", "Flexi Cap", "ELSS"));

        mockMvc.perform(get("/portfolio/funds/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Large Cap"))
                .andExpect(jsonPath("$[1]").value("Flexi Cap"))
                .andExpect(jsonPath("$[2]").value("ELSS"));
    }

    @Test
    @DisplayName("GET /portfolio/funds/search returns paginated scheme views publicly")
    void searchSchemes_public() throws Exception {
        FundService.SchemeSummaryView view = new FundService.SchemeSummaryView(
                "122639", "INF846K01164", "Parag Parikh Flexi Cap Fund", "PPFAS Mutual Fund",
                "Flexi Cap", "Equity Scheme", "Very High", 82.45, LocalDate.now(),
                1000.0, 1000.0, 24.5, 21.8, 22.4, 0.62, 68500.0, false, "BSE_122639"
        );

        when(fundService.searchSchemes(any())).thenReturn(new PageImpl<>(List.of(view), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/portfolio/funds/search").param("query", "Parag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].schemeCode").value("122639"))
                .andExpect(jsonPath("$.content[0].schemeName").value("Parag Parikh Flexi Cap Fund"));
    }

    @Test
    @DisplayName("GET /portfolio/funds/recommendations returns AI response")
    void getRecommendations_public() throws Exception {
        AiFundRecommendationService.RecommendedFundView fundView = new AiFundRecommendationService.RecommendedFundView(
                "122639", "INF846K01164", "Parag Parikh Flexi Cap Fund", "PPFAS Mutual Fund",
                "Flexi Cap", "Very High", 82.45, 1000.0, 1000.0, 24.5, 21.8, 22.4, 0.62,
                68500.0, false, 95, "TOP_PICK", "Great 3Y CAGR", "अच्छा रिटर्न", List.of("Low ER")
        );

        AiFundRecommendationService.RecommendationResponse resp = new AiFundRecommendationService.RecommendationResponse(
                "Profile Summary", "Wealth Creation Strategy", List.of(fundView), Map.of("Equity", 80, "Debt", 20)
        );

        when(fundService.getAiRecommendations(any())).thenReturn(resp);

        mockMvc.perform(get("/portfolio/funds/recommendations")
                        .param("riskAppetite", "HIGH")
                        .param("goal", "WEALTH_CREATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileSummary").value("Profile Summary"))
                .andExpect(jsonPath("$.recommendations[0].aiScore").value(95))
                .andExpect(jsonPath("$.recommendations[0].matchRating").value("TOP_PICK"));
    }

    @Test
    @DisplayName("POST /portfolio/funds/orders/purchase creates lumpsum order")
    void placeLumpsumOrder_authenticated() throws Exception {
        UUID invId = UUID.randomUUID();

        BseMfOrderService.MfOrderReceipt receipt = new BseMfOrderService.MfOrderReceipt(
                invId, USER_ID, "122639", "Parag Parikh Flexi Cap", "PURCHASE", "SUBMITTED",
                5000.0, 60.64, 82.45, "FOLIO123", "BSE_PUR_1001", "Order placed successfully", java.time.Instant.now()
        );

        when(bseOrderService.placeLumpsumOrder(eq(USER_ID), any())).thenReturn(receipt);

        String json = """
                {
                    "schemeCode": "122639",
                    "amount": 5000.0,
                    "folioNumber": "FOLIO123"
                }
                """;

        mockMvc.perform(post("/portfolio/funds/orders/purchase")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.bseOrderId").value("BSE_PUR_1001"));
    }

    @Test
    @DisplayName("GET /portfolio/funds/investments/me returns user portfolio summary")
    void getMyPortfolio_authenticated() throws Exception {
        BseMfOrderService.UserMfPortfolioSummary summary = new BseMfOrderService.UserMfPortfolioSummary(
                10000.0, 12000.0, 2000.0, 20.0, 1, List.of(), List.of()
        );

        when(bseOrderService.getUserPortfolioSummary(USER_ID)).thenReturn(summary);

        mockMvc.perform(get("/portfolio/funds/investments/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvested").value(10000.0))
                .andExpect(jsonPath("$.totalCurrentValue").value(12000.0))
                .andExpect(jsonPath("$.totalGainPct").value(20.0));
    }
}

