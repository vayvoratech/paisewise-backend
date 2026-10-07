package in.sapphirus.rupee.portfolio.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record PlaceOrderRequest(
        @NotBlank(message = "Symbol is required")
        String symbol,

        @NotBlank(message = "Side is required (BUY or SELL)")
        String side,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,

        @NotBlank(message = "Order type is required (MARKET, LIMIT, SL, SL-M)")
        String orderType,

        String product,        // CNC | MIS | NRML (default: CNC)
        String exchange,       // NSE | BSE (default: NSE)
        BigDecimal price,      // Required for LIMIT / SL
        BigDecimal triggerPrice, // Required for SL / SL-M
        String validity,       // DAY | IOC (default: DAY)
        String clientOrderId,  // Idempotency key
        String kycStatus       // Optional user KYC status override
) {}
