package in.sapphirus.rupee.portfolio.client;

import in.sapphirus.rupee.portfolio.config.FyersProperties;
import in.sapphirus.rupee.portfolio.dto.FyersOrderRequest;
import in.sapphirus.rupee.portfolio.dto.FyersOrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FyersGatewayTest {

    private FyersProperties props;
    private FyersGateway fyersGateway;

    @BeforeEach
    void setUp() {
        props = new FyersProperties();
        props.setStubMode(true);
        props.setWebhookSecret("test_webhook_secret_key_123");
        fyersGateway = new FyersGateway(props);
    }

    @Test
    @DisplayName("placeOrder in stub mode returns successful broker response")
    void placeOrder_stubMode_success() {
        FyersOrderRequest req = FyersOrderRequest.from(
                "RELIANCE", "BUY", 10, "LIMIT", "CNC",
                BigDecimal.valueOf(2500.0), null, "DAY", "CLIENT-ORD-001"
        );

        FyersOrderResponse response = fyersGateway.placeOrder(req);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.brokerOrderId()).isNotNull().startsWith("FYERS-");
        assertThat(response.message()).contains("Order submitted successfully");
    }

    @Test
    @DisplayName("cancelOrder in stub mode returns success")
    void cancelOrder_stubMode_success() {
        FyersOrderResponse response = fyersGateway.cancelOrder("FYERS-123456");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.brokerOrderId()).isEqualTo("FYERS-123456");
    }

    @Test
    @DisplayName("HMAC-SHA256 signature verification succeeds with correct secret")
    void verifyWebhookSignature_valid_signature_true() {
        String payload = "{\"tradeId\":\"TRD-1001\",\"symbol\":\"INFY\",\"price\":1500.0}";
        String signature = fyersGateway.calculateHmacSha256(payload, props.getWebhookSecret());

        boolean valid = fyersGateway.verifyWebhookSignature(payload, signature);
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("HMAC-SHA256 signature verification fails with tampered payload or wrong signature")
    void verifyWebhookSignature_invalid_signature_false() {
        String payload = "{\"tradeId\":\"TRD-1001\",\"symbol\":\"INFY\",\"price\":1500.0}";
        String tamperedPayload = "{\"tradeId\":\"TRD-1001\",\"symbol\":\"INFY\",\"price\":9999.0}";
        String signature = fyersGateway.calculateHmacSha256(payload, props.getWebhookSecret());

        boolean validWithTamperedBody = fyersGateway.verifyWebhookSignature(tamperedPayload, signature);
        assertThat(validWithTamperedBody).isFalse();

        boolean validWithBogusSig = fyersGateway.verifyWebhookSignature(payload, "invalid_bogus_signature");
        assertThat(validWithBogusSig).isFalse();

        boolean validWithNullSig = fyersGateway.verifyWebhookSignature(payload, null);
        assertThat(validWithNullSig).isFalse();
    }
}
