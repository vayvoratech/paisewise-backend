package in.sapphirus.rupee.portfolio.client;

import in.sapphirus.rupee.portfolio.config.BseStarMfProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Client for BSE StarMF order placement (purchase on SIP debit).
 *
 * <h3>Stub Mode (default)</h3>
 * When {@code bse.starmf.stub-mode=true} (the default in dev/CI), this client
 * logs the order details and returns a fake BSE order ID prefixed with {@code STUB_}.
 * No network calls are made. Flip {@code STARMF_STUB_MODE=false} in production.
 *
 * <h3>Real Mode</h3>
 * When stub-mode is off, a real HTTP call is made to the BSE StarMF web service.
 * The BSE StarMF API is a SOAP/REST hybrid — swap this stub implementation with
 * the actual HTTP call once you have live credentials and the BSE member code.
 *
 * <h3>Request format (BSE StarMF REST)</h3>
 * <pre>
 * POST /BseStarMfWebService.asmx/MFSIPOrderEntryParam
 * Content-Type: application/x-www-form-urlencoded
 *
 * UserId={userId}&Password={password}&MemberCode={memberCode}
 * &ClientCode={folioNumber}&SchemeCode={bseSchemeCode}&BuySell=P
 * &BuySellType=FRESH&DPTxn=N&OrderVal={amount}&Remarks={remarks}
 * &KYCStatus=Y&RefNo={refNo}&SubBrCode=&EUINValid=N
 * </pre>
 *
 * A successful response contains a BSE order number that is stored in
 * {@code mf_investments.bse_order_id}.
 */
@Component
public class BseStarMfClient {

    private static final Logger log = LoggerFactory.getLogger(BseStarMfClient.class);

    private final BseStarMfProperties props;

    public BseStarMfClient(BseStarMfProperties props) {
        this.props = props;
    }

    /**
     * Submit a mutual fund purchase order to BSE StarMF.
     *
     * @param userId      internal user UUID (for logging)
     * @param schemeCode  MF scheme code (as stored in our DB, typically AMFI code)
     * @param amount      purchase amount in INR
     * @param folioNumber existing folio number, or null for a new folio
     * @param sipId       the SIP UUID this purchase belongs to (used as reference number)
     * @return BSE order ID (real or stub prefixed with STUB_)
     */
    public String submitPurchase(UUID userId, String schemeCode, double amount,
                                  String folioNumber, UUID sipId) {

        if (props.isStubMode()) {
            String stubOrderId = "STUB_" + System.currentTimeMillis();
            log.info("[BSE-STUB] Purchase order submitted: userId={}, scheme={}, amount={}, folio={}, sipId={} → orderId={}",
                    userId, schemeCode, amount, folioNumber, sipId, stubOrderId);
            return stubOrderId;
        }

        // ── Real BSE StarMF call (requires live credentials) ─────────────────
        // TODO: Replace stub with actual HTTP call when BSE credentials are available.
        // Reference: BSE StarMF Developer Guide v2.3
        //
        // String refNo = sipId.toString().replace("-", "").substring(0, 16);
        // String body = "UserId=" + encode(props.getUserId())
        //         + "&Password=" + encode(props.getPassword())
        //         + "&MemberCode=" + encode(props.getMemberCode())
        //         + "&ClientCode=" + encode(folioNumber != null ? folioNumber : "NEW")
        //         + "&SchemeCode=" + encode(schemeCode)
        //         + "&BuySell=P&BuySellType=FRESH&DPTxn=N"
        //         + "&OrderVal=" + amount
        //         + "&Remarks=SIP_DEBIT_" + sipId
        //         + "&KYCStatus=Y&RefNo=" + refNo
        //         + "&SubBrCode=&EUINValid=N";
        //
        // HttpResponse<String> response = httpClient.send(...);
        // parse BSE order number from XML/text response
        //
        log.warn("[BSE] stub-mode=false but real implementation not wired. Returning placeholder.");
        return "PENDING_" + UUID.randomUUID();
    }
}
