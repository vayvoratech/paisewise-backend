package in.sapphirus.rupee.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds razorpay.* from application.yml.
 * Set RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET env vars in prod.
 */
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayProperties {

    /** Razorpay API key id (rzp_live_xxx or rzp_test_xxx). */
    private String keyId;

    /** Razorpay API key secret. */
    private String keySecret;

    /**
     * Webhook secret set in Razorpay Dashboard → Webhooks.
     * Used for HMAC-SHA256 signature verification on incoming webhook events.
     */
    private String webhookSecret;

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public String getKeySecret() { return keySecret; }
    public void setKeySecret(String keySecret) { this.keySecret = keySecret; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
}
