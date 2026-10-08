package in.sapphirus.rupee.portfolio.service;

import in.sapphirus.rupee.portfolio.domain.Holding;
import in.sapphirus.rupee.portfolio.exception.InsufficientMarginException;
import in.sapphirus.rupee.portfolio.exception.KycNotVerifiedException;
import in.sapphirus.rupee.portfolio.exception.OrderVelocityExceededException;
import in.sapphirus.rupee.portfolio.exception.PositionLimitExceededException;
import in.sapphirus.rupee.portfolio.repo.HoldingRepository;
import in.sapphirus.rupee.portfolio.repo.LedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskEngineTest {

    @Mock
    private LedgerRepository ledgerRepo;

    @Mock
    private HoldingRepository holdingRepo;

    @InjectMocks
    private RiskEngine riskEngine;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        riskEngine.resetVelocity(userId);
    }

    // ── 1. KYC Checks ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyKyc succeeds when status is VERIFIED")
    void verifyKyc_verified_success() {
        riskEngine.verifyKyc(userId, "VERIFIED");
        riskEngine.verifyKyc(userId, "verified");
    }

    @Test
    @DisplayName("verifyKyc throws KycNotVerifiedException when status is PENDING or null")
    void verifyKyc_notVerified_throwsException() {
        assertThatThrownBy(() -> riskEngine.verifyKyc(userId, "PENDING"))
                .isInstanceOf(KycNotVerifiedException.class)
                .hasMessageContaining("KYC must be VERIFIED");

        assertThatThrownBy(() -> riskEngine.verifyKyc(userId, null))
                .isInstanceOf(KycNotVerifiedException.class);

        assertThatThrownBy(() -> riskEngine.verifyKyc(userId, "REJECTED"))
                .isInstanceOf(KycNotVerifiedException.class);
    }

    // ── 2. Margin Checks ──────────────────────────────────────────────────────

    @Test
    @DisplayName("checkMargin succeeds when available balance >= required margin")
    void checkMargin_sufficientBalance_success() {
        when(ledgerRepo.calculateBalance(userId)).thenReturn(10000.0);

        // Required margin = 10 * 500 = 5000 <= 10000
        riskEngine.checkMargin(userId, "BUY", 10, BigDecimal.valueOf(500.0));
    }

    @Test
    @DisplayName("checkMargin throws InsufficientMarginException when balance is lower")
    void checkMargin_insufficientBalance_throwsException() {
        when(ledgerRepo.calculateBalance(userId)).thenReturn(2000.0);

        // Required margin = 10 * 500 = 5000 > 2000
        assertThatThrownBy(() -> riskEngine.checkMargin(userId, "BUY", 10, BigDecimal.valueOf(500.0)))
                .isInstanceOf(InsufficientMarginException.class)
                .hasMessageContaining("Insufficient margin");
    }

    @Test
    @DisplayName("checkMargin skipped for SELL orders")
    void checkMargin_sellOrder_skipped() {
        riskEngine.checkMargin(userId, "SELL", 10, BigDecimal.valueOf(500.0));
        verifyNoInteractions(ledgerRepo);
    }

    // ── 3. Open Position Limits (50 positions) ────────────────────────────────

    @Test
    @DisplayName("checkOpenPositionLimit succeeds when open positions < 50")
    void checkOpenPositionLimit_underLimit_success() {
        when(holdingRepo.findByUserIdAndSymbol(userId, "TCS")).thenReturn(Optional.empty());
        when(holdingRepo.countOpenPositionsByUserId(userId)).thenReturn(40L);

        riskEngine.checkOpenPositionLimit(userId, "TCS", "BUY");
    }

    @Test
    @DisplayName("checkOpenPositionLimit succeeds for existing holding even if positions count = 50")
    void checkOpenPositionLimit_existingHolding_success() {
        Holding existing = new Holding(userId, "INFY", 10, 1500.0, 15000.0, "CNC", false);
        when(holdingRepo.findByUserIdAndSymbol(userId, "INFY")).thenReturn(Optional.of(existing));

        // Should not throw because user already holds INFY
        riskEngine.checkOpenPositionLimit(userId, "INFY", "BUY");
        verify(holdingRepo, never()).countOpenPositionsByUserId(userId);
    }

    @Test
    @DisplayName("checkOpenPositionLimit throws PositionLimitExceededException on 51st position")
    void checkOpenPositionLimit_exceeded_throwsException() {
        when(holdingRepo.findByUserIdAndSymbol(userId, "NEW_SYM")).thenReturn(Optional.empty());
        when(holdingRepo.countOpenPositionsByUserId(userId)).thenReturn(50L);

        assertThatThrownBy(() -> riskEngine.checkOpenPositionLimit(userId, "NEW_SYM", "BUY"))
                .isInstanceOf(PositionLimitExceededException.class)
                .hasMessageContaining("Open position limit reached");
    }

    // ── 4. Order Velocity (10/min) ────────────────────────────────────────────

    @Test
    @DisplayName("checkOrderVelocity allows 10 orders per minute and blocks 11th")
    void checkOrderVelocity_rateLimiting() {
        // Place 10 orders within 1 minute
        for (int i = 0; i < 10; i++) {
            riskEngine.checkOrderVelocity(userId);
            riskEngine.recordOrderAttempt(userId);
        }

        // 11th order should be blocked
        assertThatThrownBy(() -> riskEngine.checkOrderVelocity(userId))
                .isInstanceOf(OrderVelocityExceededException.class)
                .hasMessageContaining("Order velocity limit exceeded");
    }

    // ── Full Validation Flow ──────────────────────────────────────────────────

    @Test
    @DisplayName("validateOrder runs full flow successfully")
    void validateOrder_allChecksPass_success() {
        when(holdingRepo.findByUserIdAndSymbol(userId, "RELIANCE")).thenReturn(Optional.empty());
        when(holdingRepo.countOpenPositionsByUserId(userId)).thenReturn(10L);
        when(ledgerRepo.calculateBalance(userId)).thenReturn(50000.0);

        riskEngine.validateOrder(userId, "RELIANCE", "BUY", 5, BigDecimal.valueOf(2500.0), "VERIFIED");
    }
}
