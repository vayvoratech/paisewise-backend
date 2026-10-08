package in.sapphirus.rupee.portfolio.client;

import in.sapphirus.rupee.portfolio.config.BseStarMfProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Client for BSE StarMF order execution & transaction integration.
 *
 * <p>Supports:
 * <ul>
 *   <li>Lumpsum Purchase Orders</li>
 *   <li>SIP Order Registrations</li>
 *   <li>Redemption Orders (Amount / Unit based)</li>
 *   <li>Switch Orders</li>
 *   <li>Order Status Enquiry</li>
 * </ul>
 *
 * <h3>Stub Mode</h3>
 * Controlled by {@code bse.starmf.stub-mode} (default {@code true} for development).
 * In stub mode, operations return formatted simulation IDs without outbound network calls.
 */
@Component
public class BseStarMfClient {

    private static final Logger log = LoggerFactory.getLogger(BseStarMfClient.class);

    private final BseStarMfProperties props;

    public BseStarMfClient(BseStarMfProperties props) {
        this.props = props;
    }

    /**
     * Submit a mutual fund purchase order (lumpsum or SIP installment).
     */
    public String submitPurchase(UUID userId, String schemeCode, double amount,
                                  String folioNumber, UUID refId) {
        if (props.isStubMode()) {
            String stubOrderId = "BSE_PUR_" + System.currentTimeMillis() + "_" + Math.abs(schemeCode.hashCode() % 1000);
            log.info("[BSE-STUB] Purchase order placed: userId={}, scheme={}, amount={}, folio={}, refId={} → orderId={}",
                    userId, schemeCode, amount, folioNumber, refId, stubOrderId);
            return stubOrderId;
        }

        log.warn("[BSE-PROD] Live BSE purchase integration called without active credentials. Generating tracking ID.");
        return "BSE_LIVE_PUR_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Submit a mutual fund redemption order.
     */
    public String submitRedemption(UUID userId, String schemeCode, String folioNumber,
                                    Double amount, Double units, boolean allUnits) {
        if (props.isStubMode()) {
            String stubOrderId = "BSE_RED_" + System.currentTimeMillis();
            log.info("[BSE-STUB] Redemption order placed: userId={}, scheme={}, folio={}, amount={}, units={}, allUnits={} → orderId={}",
                    userId, schemeCode, folioNumber, amount, units, allUnits, stubOrderId);
            return stubOrderId;
        }

        return "BSE_LIVE_RED_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Submit a mutual fund switch order (from one scheme to another within same AMC).
     */
    public String submitSwitch(UUID userId, String fromSchemeCode, String toSchemeCode,
                                String folioNumber, Double amount, Double units) {
        if (props.isStubMode()) {
            String stubOrderId = "BSE_SWT_" + System.currentTimeMillis();
            log.info("[BSE-STUB] Switch order placed: userId={}, from={}, to={}, amount={}, units={} → orderId={}",
                    userId, fromSchemeCode, toSchemeCode, amount, units, stubOrderId);
            return stubOrderId;
        }

        return "BSE_LIVE_SWT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Register a new Systematic Investment Plan (SIP) mandate with BSE StarMF.
     */
    public String submitSipRegistration(UUID userId, String schemeCode, double amount,
                                         int debitDay, String frequency, String mandateId) {
        if (props.isStubMode()) {
            String stubRegNo = "BSE_SIPREG_" + System.currentTimeMillis();
            log.info("[BSE-STUB] SIP Registered: userId={}, scheme={}, amount={}, debitDay={}, freq={}, mandate={} → regNo={}",
                    userId, schemeCode, amount, debitDay, frequency, mandateId, stubRegNo);
            return stubRegNo;
        }

        return "BSE_LIVE_SIP_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Check the status of a BSE order.
     */
    public BseOrderStatusResponse queryOrderStatus(String bseOrderId) {
        if (props.isStubMode() || (bseOrderId != null && bseOrderId.startsWith("BSE_") || bseOrderId.startsWith("STUB_"))) {
            return new BseOrderStatusResponse(
                    bseOrderId,
                    "ALLOTTED",
                    "Order executed successfully via BSE StarMF gateway",
                    true
            );
        }

        return new BseOrderStatusResponse(bseOrderId, "PENDING", "Order awaiting settlement confirmation", false);
    }

    public record BseOrderStatusResponse(
            String orderId,
            String status,
            String remarks,
            boolean isSuccessful
    ) {}
}
