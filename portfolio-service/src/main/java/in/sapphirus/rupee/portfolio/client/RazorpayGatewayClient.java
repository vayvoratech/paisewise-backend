package in.sapphirus.rupee.portfolio.client;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Subscription;
import com.razorpay.Utils;
import in.sapphirus.rupee.portfolio.config.RazorpayProperties;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Thin Spring wrapper around the Razorpay Java SDK.
 *
 * Responsibilities:
 *   - Pause / Cancel / Resume Razorpay Subscriptions (maps to UPI AutoPay mandates)
 *   - Verify HMAC-SHA256 webhook signatures
 *
 * Thread safety: RazorpayClient is created once and reused (SDK is thread-safe).
 */
@Component
public class RazorpayGatewayClient implements RazorpayOperations {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGatewayClient.class);

    private final RazorpayProperties props;
    private RazorpayClient razorpay;

    public RazorpayGatewayClient(RazorpayProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        try {
            razorpay = new RazorpayClient(props.getKeyId(), props.getKeySecret());
            log.info("Razorpay client initialised with key-id={}", props.getKeyId());
        } catch (RazorpayException e) {
            // Log and continue — service starts but Razorpay calls will fail gracefully
            log.error("Failed to initialise Razorpay client: {}", e.getMessage());
        }
    }

    /**
     * Pause a Razorpay Subscription.
     *
     * @param subscriptionId Razorpay subscription ID (sub_xxx)
     * @throws RazorpayException if the API call fails
     */
    public void pauseSubscription(String subscriptionId) throws RazorpayException {
        log.info("Pausing Razorpay subscription={}", subscriptionId);
        JSONObject req = new JSONObject();
        req.put("pause_at", "now");
        razorpay.subscriptions.pause(subscriptionId, req);
        log.info("Razorpay subscription={} paused successfully", subscriptionId);
    }

    /**
     * Resume a previously paused Razorpay Subscription.
     *
     * @param subscriptionId Razorpay subscription ID (sub_xxx)
     * @throws RazorpayException if the API call fails
     */
    public void resumeSubscription(String subscriptionId) throws RazorpayException {
        log.info("Resuming Razorpay subscription={}", subscriptionId);
        JSONObject req = new JSONObject();
        req.put("resume_at", "now");
        razorpay.subscriptions.resume(subscriptionId, req);
        log.info("Razorpay subscription={} resumed successfully", subscriptionId);
    }

    /**
     * Cancel a Razorpay Subscription immediately.
     *
     * @param subscriptionId Razorpay subscription ID (sub_xxx)
     * @throws RazorpayException if the API call fails
     */
    public void cancelSubscription(String subscriptionId) throws RazorpayException {
        log.info("Cancelling Razorpay subscription={}", subscriptionId);
        JSONObject req = new JSONObject();
        req.put("cancel_at_cycle_end", 0); // cancel immediately
        Subscription result = razorpay.subscriptions.cancel(subscriptionId, req);
        log.info("Razorpay subscription={} cancelled, status={}", subscriptionId, result.get("status"));
    }

    /**
     * Fetch a Razorpay Subscription by ID.
     *
     * @param subscriptionId Razorpay subscription ID
     * @return Subscription object from SDK
     * @throws RazorpayException if the API call fails
     */
    public Subscription fetchSubscription(String subscriptionId) throws RazorpayException {
        return razorpay.subscriptions.fetch(subscriptionId);
    }

    /**
     * Verify the HMAC-SHA256 signature on a Razorpay webhook event.
     *
     * Razorpay sends:  X-Razorpay-Signature: HMAC-SHA256(rawBody, webhookSecret)
     *
     * @param rawBody   the raw request body as a string (must NOT be parsed first)
     * @param signature the value of X-Razorpay-Signature header
     * @return true if the signature is valid
     */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        try {
            Utils.verifyWebhookSignature(rawBody, signature, props.getWebhookSecret());
            return true;
        } catch (RazorpayException e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
