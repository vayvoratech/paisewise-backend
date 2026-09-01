package in.sapphirus.rupee.practice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.sapphirus.rupee.practice.domain.PriceAlert;
import in.sapphirus.rupee.practice.domain.Stock;
import in.sapphirus.rupee.practice.dto.CreateAlertRequest;
import in.sapphirus.rupee.practice.repo.PriceAlertRepository;
import in.sapphirus.rupee.practice.repo.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PriceAlertServiceTest {

    private final PriceAlertRepository repository = mock(PriceAlertRepository.class);
    private final StockRepository stockRepository = mock(StockRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private PriceAlertService service;

    @BeforeEach
    void setUp() {
        reset(repository, stockRepository);
        service = new PriceAlertService(repository, stockRepository, objectMapper);
    }

    @Test
    void createAlert_success_for_GT() {
        UUID userId = UUID.randomUUID();
        Stock stock = new Stock("RELIANCE", "Reliance Industries Ltd.", 2952.0, 1.2, "🛢️", "[]");
        when(stockRepository.findById("RELIANCE")).thenReturn(Optional.of(stock));

        CreateAlertRequest req = new CreateAlertRequest("RELIANCE", "GT", BigDecimal.valueOf(3000.0), "High target", null);
        PriceAlert savedMock = new PriceAlert(userId, "RELIANCE", "GT", BigDecimal.valueOf(3000.0), BigDecimal.valueOf(2952.0), "High target", null);
        when(repository.save(any(PriceAlert.class))).thenReturn(savedMock);

        PriceAlert result = service.createAlert(userId, req);

        assertThat(result).isNotNull();
        assertThat(result.getTargetPrice()).isEqualTo(BigDecimal.valueOf(3000.0));
        verify(repository, times(1)).save(any(PriceAlert.class));
    }

    @Test
    void createAlert_fail_for_GT_price_too_low() {
        UUID userId = UUID.randomUUID();
        Stock stock = new Stock("RELIANCE", "Reliance Industries Ltd.", 2952.0, 1.2, "🛢️", "[]");
        when(stockRepository.findById("RELIANCE")).thenReturn(Optional.of(stock));

        CreateAlertRequest req = new CreateAlertRequest("RELIANCE", "GT", BigDecimal.valueOf(2900.0), "Invalid target", null);

        assertThatThrownBy(() -> service.createAlert(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Target price must be greater than current price");

        verify(repository, never()).save(any(PriceAlert.class));
    }

    @Test
    void createAlert_success_for_LT() {
        UUID userId = UUID.randomUUID();
        Stock stock = new Stock("RELIANCE", "Reliance Industries Ltd.", 2952.0, 1.2, "🛢️", "[]");
        when(stockRepository.findById("RELIANCE")).thenReturn(Optional.of(stock));

        CreateAlertRequest req = new CreateAlertRequest("RELIANCE", "LT", BigDecimal.valueOf(2900.0), "Low target", null);
        PriceAlert savedMock = new PriceAlert(userId, "RELIANCE", "LT", BigDecimal.valueOf(2900.0), BigDecimal.valueOf(2952.0), "Low target", null);
        when(repository.save(any(PriceAlert.class))).thenReturn(savedMock);

        PriceAlert result = service.createAlert(userId, req);

        assertThat(result).isNotNull();
        assertThat(result.getTargetPrice()).isEqualTo(BigDecimal.valueOf(2900.0));
    }

    @Test
    void createAlert_fail_for_LT_price_too_high() {
        UUID userId = UUID.randomUUID();
        Stock stock = new Stock("RELIANCE", "Reliance Industries Ltd.", 2952.0, 1.2, "🛢️", "[]");
        when(stockRepository.findById("RELIANCE")).thenReturn(Optional.of(stock));

        CreateAlertRequest req = new CreateAlertRequest("RELIANCE", "LT", BigDecimal.valueOf(3000.0), "Invalid target", null);

        assertThatThrownBy(() -> service.createAlert(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Target price must be less than current price");
    }

    @Test
    void getUserAlerts_returnsList() {
        UUID userId = UUID.randomUUID();
        List<PriceAlert> list = List.of(new PriceAlert(), new PriceAlert());
        when(repository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(list);

        List<PriceAlert> result = service.getUserAlerts(userId);

        assertThat(result).hasSize(2);
        verify(repository, times(1)).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void deleteAlert_success() {
        UUID userId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        PriceAlert alert = new PriceAlert();
        alert.setId(alertId);
        alert.setUserId(userId);
        alert.setStatus("ACTIVE");

        when(repository.findById(alertId)).thenReturn(Optional.of(alert));

        service.deleteAlert(userId, alertId);

        assertThat(alert.getStatus()).isEqualTo("CANCELLED");
        verify(repository, times(1)).save(alert);
    }

    @Test
    void deleteAlert_accessDenied() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        PriceAlert alert = new PriceAlert();
        alert.setId(alertId);
        alert.setUserId(otherUserId);

        when(repository.findById(alertId)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> service.deleteAlert(userId, alertId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Access denied");

        verify(repository, never()).save(any(PriceAlert.class));
    }

    @Test
    void processAlerts_triggersGT() throws Exception {
        PriceAlert alert = new PriceAlert();
        alert.setId(UUID.randomUUID());
        alert.setSymbol("NSE:RELIANCE");
        alert.setCondition("GT");
        alert.setTargetPrice(BigDecimal.valueOf(3000.0));
        alert.setStatus("ACTIVE");

        List<PriceAlert> list = new ArrayList<>(List.of(alert));
        when(repository.findActiveAlertsForSymbol("NSE:RELIANCE")).thenReturn(list);

        String message = "{\"symbol\":\"NSE:RELIANCE\",\"ltp\":3050.0}";
        service.processAlerts(message);

        ArgumentCaptor<PriceAlert> captor = ArgumentCaptor.forClass(PriceAlert.class);
        verify(repository, times(1)).save(captor.capture());

        PriceAlert saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("TRIGGERED");
        assertThat(saved.getTriggeredPrice()).isEqualTo(BigDecimal.valueOf(3050.0));
        assertThat(saved.getTriggeredAt()).isNotNull();
    }

    @Test
    void processAlerts_skipsExpiredAlert() {
        PriceAlert alert = new PriceAlert();
        alert.setId(UUID.randomUUID());
        alert.setSymbol("NSE:RELIANCE");
        alert.setCondition("GT");
        alert.setTargetPrice(BigDecimal.valueOf(3000.0));
        alert.setStatus("ACTIVE");
        alert.setExpiresAt(Instant.now().minus(java.time.Duration.ofHours(1))); // expired 1 hour ago

        List<PriceAlert> list = new ArrayList<>(List.of(alert));
        when(repository.findActiveAlertsForSymbol("NSE:RELIANCE")).thenReturn(list);

        String message = "{\"symbol\":\"NSE:RELIANCE\",\"ltp\":3050.0}";
        service.processAlerts(message);

        ArgumentCaptor<PriceAlert> captor = ArgumentCaptor.forClass(PriceAlert.class);
        verify(repository, times(1)).save(captor.capture());

        PriceAlert saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("EXPIRED");
        assertThat(saved.getTriggeredPrice()).isNull();
        assertThat(saved.getTriggeredAt()).isNull();
    }
}
