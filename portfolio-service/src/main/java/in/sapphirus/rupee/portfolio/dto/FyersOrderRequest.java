package in.sapphirus.rupee.portfolio.dto;

import java.math.BigDecimal;

public record FyersOrderRequest(
        String symbol,
        int qty,
        int type,           // 1: Limit, 2: Market, 3: Stop order, 4: Stoplimit
        int side,           // 1: Buy, -1: Sell
        String productType, // CNC, INTRADAY, MARGIN
        BigDecimal limitPrice,
        BigDecimal stopPrice,
        String validity,    // DAY, IOC
        boolean offlineOrder,
        String clientOrderId
) {
    public static FyersOrderRequest from(String symbol, String sideStr, int qty, String orderTypeStr,
                                         String productStr, BigDecimal price, BigDecimal triggerPrice,
                                         String validityStr, String clientOrderId) {
        int side = "BUY".equalsIgnoreCase(sideStr) ? 1 : -1;
        int type = switch (orderTypeStr.toUpperCase()) {
            case "LIMIT" -> 1;
            case "SL" -> 3;
            case "SL-M" -> 4;
            default -> 2; // MARKET
        };
        String product = switch (productStr != null ? productStr.toUpperCase() : "CNC") {
            case "MIS" -> "INTRADAY";
            case "NRML" -> "MARGIN";
            default -> "CNC";
        };
        String fyersSymbol = symbol.contains(":") ? symbol : "NSE:" + symbol + "-EQ";
        return new FyersOrderRequest(
                fyersSymbol,
                qty,
                type,
                side,
                product,
                price,
                triggerPrice,
                validityStr != null ? validityStr : "DAY",
                false,
                clientOrderId
        );
    }
}
