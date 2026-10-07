package in.sapphirus.rupee.portfolio.client;

import in.sapphirus.rupee.portfolio.config.FyersProperties;
import in.sapphirus.rupee.portfolio.dto.FyersOrderRequest;
import in.sapphirus.rupee.portfolio.dto.FyersOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Gateway client for Fyers Broker REST APIs (v3).
 *
 * Provides:
 * - Order Placement (POST /orders/sync)
 * - Order Cancellation (DELETE /orders/sync)
 * - Order Status retrieval
 * - HMAC-SHA256 signature verification for broker webhook callbacks
 * - Stub mode for safe local testing and dev environments
 */
@Component
public class FyersGateway {

    private static final Logger log = LoggerFactory.getLogger(FyersGateway.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final FyersProperties props;
    private final RestClient restClient;

    public FyersGateway(FyersProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Submit an order to the Fyers Broker API.
     *
     * @param request order details formatted for Fyers
     * @return broker response with brokerOrderId and status
     */
    public FyersOrderResponse placeOrder(FyersOrderRequest request) {
        if (props.isStubMode()) {
            String stubBrokerOrderId = "FYERS-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            log.info("[FYERS-STUB] Placed order for symbol={}, qty={}, type={}, side={}, clientOrderId={} -> brokerOrderId={}",
                    request.symbol(), request.qty(), request.type(), request.side(), request.clientOrderId(), stubBrokerOrderId);
            return FyersOrderResponse.ok(stubBrokerOrderId, "Order submitted successfully to Fyers (stub)");
        }

        try {
            log.info("Sending placeOrder request to Fyers API: symbol={}, qty={}, clientOrderId={}",
                    request.symbol(), request.qty(), request.clientOrderId());

            String authHeader = props.getAppId() + ":" + props.getAccessToken();

            FyersOrderResponse response = restClient.post()
                    .uri("/orders/sync")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .body(request)
                    .retrieve()
                    .body(FyersOrderResponse.class);

            if (response == null) {
                return FyersOrderResponse.error(500, "Empty response from Fyers gateway");
            }
            return response;

        } catch (Exception e) {
            log.error("Failed to place order on Fyers API: {}", e.getMessage(), e);
            return FyersOrderResponse.error(500, "Fyers gateway error: " + e.getMessage());
        }
    }

    /**
     * Cancel an active order with Fyers.
     */
    public FyersOrderResponse cancelOrder(String brokerOrderId) {
        if (props.isStubMode()) {
            log.info("[FYERS-STUB] Cancelled brokerOrderId={}", brokerOrderId);
            return FyersOrderResponse.ok(brokerOrderId, "Order cancelled successfully (stub)");
        }

        try {
            String authHeader = props.getAppId() + ":" + props.getAccessToken();
            return restClient.delete()
                    .uri("/orders/sync/{id}", brokerOrderId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(FyersOrderResponse.class);
        } catch (Exception e) {
            log.error("Failed to cancel order on Fyers API: {}", e.getMessage(), e);
            return FyersOrderResponse.error(500, "Fyers cancel error: " + e.getMessage());
        }
    }

    /**
     * Fetch order status from Fyers.
     */
    public FyersOrderResponse getOrderStatus(String brokerOrderId) {
        if (props.isStubMode()) {
            log.info("[FYERS-STUB] Query order status for brokerOrderId={}", brokerOrderId);
            return FyersOrderResponse.ok(brokerOrderId, "Order is OPEN (stub)");
        }

        try {
            String authHeader = props.getAppId() + ":" + props.getAccessToken();
            return restClient.get()
                    .uri("/orders/sync/{id}", brokerOrderId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(FyersOrderResponse.class);
        } catch (Exception e) {
            log.error("Failed to get order status from Fyers: {}", e.getMessage(), e);
            return FyersOrderResponse.error(500, "Fyers status error: " + e.getMessage());
        }
    }

    /**
     * Verify HMAC-SHA256 signature for Fyers Webhook callbacks.
     *
     * @param rawBody raw HTTP request body
     * @param signature hex-encoded signature from header
     * @return true if valid
     */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (signature == null || signature.isBlank() || rawBody == null) {
            return false;
        }

        try {
            String expected = calculateHmacSha256(rawBody, props.getWebhookSecret());
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("HMAC signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Calculate HMAC-SHA256 hex string for given payload and secret.
     */
    public String calculateHmacSha256(String data, String secret) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", e);
        }
    }

    public FyersProperties getProperties() {
        return props;
    }
}
