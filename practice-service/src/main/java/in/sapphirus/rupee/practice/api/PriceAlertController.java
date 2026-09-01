package in.sapphirus.rupee.practice.api;

import in.sapphirus.rupee.practice.domain.PriceAlert;
import in.sapphirus.rupee.practice.dto.CreateAlertRequest;
import in.sapphirus.rupee.practice.service.PriceAlertService;
import in.sapphirus.rupee.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/practice/alerts")
public class PriceAlertController {

    private final PriceAlertService alertService;

    public PriceAlertController(PriceAlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PriceAlert createAlert(@Valid @RequestBody CreateAlertRequest request) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return alertService.createAlert(userId, request);
    }

    @GetMapping
    public List<PriceAlert> getUserAlerts() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return alertService.getUserAlerts(userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlert(@PathVariable UUID id) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        alertService.deleteAlert(userId, id);
    }
}
