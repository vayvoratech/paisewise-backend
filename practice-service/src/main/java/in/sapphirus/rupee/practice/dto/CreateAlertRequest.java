package in.sapphirus.rupee.practice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateAlertRequest(
    @NotBlank(message = "Symbol is required")
    String symbol,

    @NotBlank(message = "Condition is required")
    String condition, // GT, GTE, LT, LTE

    @NotNull(message = "Target price is required")
    @DecimalMin(value = "0.01", message = "Target price must be positive")
    BigDecimal targetPrice,

    String note,

    Instant expiresAt
) {}
