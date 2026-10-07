package in.sapphirus.rupee.portfolio.service;

import in.sapphirus.rupee.portfolio.client.BseStarMfClient;
import in.sapphirus.rupee.portfolio.domain.MfInvestment;
import in.sapphirus.rupee.portfolio.domain.MfScheme;
import in.sapphirus.rupee.portfolio.repo.MfInvestmentRepository;
import in.sapphirus.rupee.portfolio.repo.MfSchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BseMfOrderService Tests")
class BseMfOrderServiceTest {

    @Mock
    private MfInvestmentRepository investmentRepo;

    @Mock
    private MfSchemeRepository schemeRepo;

    @Mock
    private BseStarMfClient bseClient;

    private BseMfOrderService orderService;
    private final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderService = new BseMfOrderService(investmentRepo, schemeRepo, bseClient);
    }

    private MfScheme sampleScheme(String code, String name, double nav, double minLump) {
        MfScheme s = new MfScheme(code, name, "AMC", "Large Cap", nav);
        s.setMinLumpsum(minLump);
        s.setActive(true);
        return s;
    }

    @Test
    @DisplayName("placeLumpsumOrder successfully submits order to BSE StarMF")
    void placeLumpsumOrder_success() {
        MfScheme scheme = sampleScheme("120503", "Mirae Asset Large Cap", 100.0, 1000.0);
        when(schemeRepo.findById("120503")).thenReturn(Optional.of(scheme));
        when(investmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bseClient.submitPurchase(eq(USER_ID), eq("120503"), eq(5000.0), any(), any()))
                .thenReturn("BSE_PUR_9999");

        BseMfOrderService.LumpsumOrderRequest req = new BseMfOrderService.LumpsumOrderRequest("120503", 5000.0, null);
        BseMfOrderService.MfOrderReceipt receipt = orderService.placeLumpsumOrder(USER_ID, req);

        assertThat(receipt).isNotNull();
        assertThat(receipt.status()).isEqualTo("SUBMITTED");
        assertThat(receipt.bseOrderId()).isEqualTo("BSE_PUR_9999");
        assertThat(receipt.unitsAllotted()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("placeLumpsumOrder throws error if amount is below minLumpsum")
    void placeLumpsumOrder_belowMinAmount() {
        MfScheme scheme = sampleScheme("120503", "Mirae Asset Large Cap", 100.0, 1000.0);
        when(schemeRepo.findById("120503")).thenReturn(Optional.of(scheme));

        BseMfOrderService.LumpsumOrderRequest req = new BseMfOrderService.LumpsumOrderRequest("120503", 500.0, null);

        assertThatThrownBy(() -> orderService.placeLumpsumOrder(USER_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum lumpsum purchase");
    }

    @Test
    @DisplayName("placeRedemptionOrder successfully submits redemption to BSE")
    void placeRedemptionOrder_success() {
        MfScheme scheme = sampleScheme("120503", "Mirae Asset Large Cap", 100.0, 1000.0);
        when(schemeRepo.findById("120503")).thenReturn(Optional.of(scheme));
        when(investmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bseClient.submitRedemption(eq(USER_ID), eq("120503"), any(), eq(2000.0), any(), eq(false)))
                .thenReturn("BSE_RED_1234");

        BseMfOrderService.RedemptionOrderRequest req = new BseMfOrderService.RedemptionOrderRequest("120503", "FOLIO1", 2000.0, null, false);
        BseMfOrderService.MfOrderReceipt receipt = orderService.placeRedemptionOrder(USER_ID, req);

        assertThat(receipt.status()).isEqualTo("SUBMITTED");
        assertThat(receipt.transactionType()).isEqualTo("REDEMPTION");
    }

    @Test
    @DisplayName("getUserPortfolioSummary correctly aggregates holdings, P&L and values")
    void getUserPortfolioSummary_aggregates() {
        MfScheme scheme = sampleScheme("120503", "Mirae Asset Large Cap", 120.0, 1000.0);
        when(schemeRepo.findById("120503")).thenReturn(Optional.of(scheme));

        MfInvestment inv = new MfInvestment(USER_ID, "120503", 10000.0);
        inv.setTransactionType("PURCHASE");
        inv.setStatus("ALLOTTED");
        inv.setUnitsAllotted(100.0); // 100 units bought for 10,000 (avg cost 100)

        when(investmentRepo.findByUserIdOrderByTransactionDateDesc(USER_ID)).thenReturn(List.of(inv));

        BseMfOrderService.UserMfPortfolioSummary summary = orderService.getUserPortfolioSummary(USER_ID);

        assertThat(summary.totalInvested()).isEqualTo(10000.0);
        assertThat(summary.totalCurrentValue()).isEqualTo(12000.0); // 100 units * NAV 120
        assertThat(summary.totalGainAbs()).isEqualTo(2000.0);
        assertThat(summary.totalGainPct()).isEqualTo(20.0);
        assertThat(summary.totalSchemesCount()).isEqualTo(1);
    }
}
