package in.sapphirus.rupee.portfolio.client;

import com.razorpay.RazorpayException;
import com.razorpay.Subscription;

/**
 * Abstraction for Razorpay operations used in SIP management.
 * Extracting an interface makes the implementation mockable in unit tests
 * without relying on byte-buddy class subclassing (which requires Java ≤ 23
 * with the bundled Mockito version).
 */
public interface RazorpayOperations {

    /** Pause a Razorpay Subscription immediately. */
    void pauseSubscription(String subscriptionId) throws RazorpayException;

    /** Resume a previously paused Razorpay Subscription. */
    void resumeSubscription(String subscriptionId) throws RazorpayException;

    /** Cancel a Razorpay Subscription immediately. */
    void cancelSubscription(String subscriptionId) throws RazorpayException;

    /** Fetch subscription details. */
    Subscription fetchSubscription(String subscriptionId) throws RazorpayException;

    /**
     * Verify Razorpay HMAC-SHA256 webhook signature.
     *
     * @param rawBody   raw request body (must not be pre-parsed)
     * @param signature value of X-Razorpay-Signature header
     * @return true if authentic
     */
    boolean verifyWebhookSignature(String rawBody, String signature);
}
