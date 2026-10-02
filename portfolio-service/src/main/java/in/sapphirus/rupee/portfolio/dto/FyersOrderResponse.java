package in.sapphirus.rupee.portfolio.dto;

public record FyersOrderResponse(
        String s,              // "ok" or "error"
        int code,              // 1101, etc.
        String message,
        String id,             // Fyers broker order ID e.g. "23091100012345"
        String brokerOrderId
) {
    public static FyersOrderResponse ok(String brokerOrderId, String message) {
        return new FyersOrderResponse("ok", 1101, message, brokerOrderId, brokerOrderId);
    }

    public static FyersOrderResponse error(int code, String message) {
        return new FyersOrderResponse("error", code, message, null, null);
    }

    public boolean isSuccess() {
        return "ok".equalsIgnoreCase(s) || (code >= 1000 && code < 1200);
    }
}
