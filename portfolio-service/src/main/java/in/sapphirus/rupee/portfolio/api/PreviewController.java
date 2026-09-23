package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.config.RazorpayProperties;
import in.sapphirus.rupee.portfolio.domain.MfScheme;
import in.sapphirus.rupee.portfolio.domain.Sip;
import in.sapphirus.rupee.portfolio.domain.SipWebhookEvent;
import in.sapphirus.rupee.portfolio.repo.MfSchemeRepository;
import in.sapphirus.rupee.portfolio.repo.SipRepository;
import in.sapphirus.rupee.portfolio.repo.SipWebhookEventRepository;
import in.sapphirus.rupee.portfolio.service.SipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/preview")
public class PreviewController {

    private final SipRepository sipRepository;
    private final SipWebhookEventRepository webhookEventRepository;
    private final MfSchemeRepository mfSchemeRepository;
    private final SipService sipService;
    private final RazorpayWebhookController webhookController;
    private final RazorpayProperties razorpayProperties;

    private static final UUID DEMO_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String DEMO_SCHEME_CODE = "INF179K01AA4";

    public PreviewController(SipRepository sipRepository,
                             SipWebhookEventRepository webhookEventRepository,
                             MfSchemeRepository mfSchemeRepository,
                             SipService sipService,
                             RazorpayWebhookController webhookController,
                             RazorpayProperties razorpayProperties) {
        this.sipRepository = sipRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.mfSchemeRepository = mfSchemeRepository;
        this.sipService = sipService;
        this.webhookController = webhookController;
        this.razorpayProperties = razorpayProperties;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("service", "portfolio-service");
        map.put("status", "UP");
        map.put("port", 8085);
        map.put("eurekaUrl", "http://localhost:8761");
        map.put("sipCount", sipRepository.count());
        map.put("webhookEventCount", webhookEventRepository.count());
        map.put("timestamp", new Date());
        return ResponseEntity.ok(map);
    }

    @GetMapping("/sips")
    public ResponseEntity<List<Sip>> getSips() {
        return ResponseEntity.ok(sipRepository.findAll());
    }

    @PostMapping("/sips/seed")
    public ResponseEntity<Sip> seedDemoSip() {
        if (!mfSchemeRepository.existsById(DEMO_SCHEME_CODE)) {
            MfScheme scheme = new MfScheme(
                    DEMO_SCHEME_CODE,
                    "Axis Bluechip Fund - Direct Growth",
                    "Axis Mutual Fund",
                    "Equity",
                    52.35
            );
            scheme.setSchemeType("Open Ended");
            mfSchemeRepository.save(scheme);
        }

        String subId = "sub_demo_" + System.currentTimeMillis();
        String mandateId = "mandate_demo_" + System.currentTimeMillis();

        Sip sip = new Sip(DEMO_USER_ID, DEMO_SCHEME_CODE, 2500.0);
        sip.setStatus("PENDING_MANDATE");
        sip.setRazorpaySubscriptionId(subId);
        sip.setUpiMandateId(mandateId);
        sip.setNextDebitDate(LocalDate.now().plusDays(7));
        return ResponseEntity.ok(sipRepository.save(sip));
    }

    @PostMapping("/sips/{id}/pause")
    public ResponseEntity<Sip> pauseSip(@PathVariable UUID id, @RequestParam(defaultValue = "Paused via preview dashboard") String reason) {
        Sip sip = sipRepository.findById(id).orElseThrow();
        sip = sipService.pauseSip(sip.getUserId(), id, reason);
        return ResponseEntity.ok(sip);
    }

    @PostMapping("/sips/{id}/resume")
    public ResponseEntity<Sip> resumeSip(@PathVariable UUID id) {
        Sip sip = sipRepository.findById(id).orElseThrow();
        sip = sipService.resumeSip(sip.getUserId(), id);
        return ResponseEntity.ok(sip);
    }

    @PostMapping("/sips/{id}/cancel")
    public ResponseEntity<Sip> cancelSip(@PathVariable UUID id, @RequestParam(defaultValue = "Cancelled via preview dashboard") String reason) {
        Sip sip = sipRepository.findById(id).orElseThrow();
        sip = sipService.cancelSip(sip.getUserId(), id, reason);
        return ResponseEntity.ok(sip);
    }

    @GetMapping("/webhook-events")
    public ResponseEntity<List<SipWebhookEvent>> getWebhookEvents() {
        return ResponseEntity.ok(webhookEventRepository.findTop20ByOrderByProcessedAtDesc());
    }

    @PostMapping("/simulate-webhook")
    public ResponseEntity<Map<String, Object>> simulateWebhook(
            @RequestParam String eventType,
            @RequestParam(required = false) String subId,
            @RequestParam(required = false) String mandateId,
            @RequestParam(required = false, defaultValue = "2500.0") double amount) {

        String targetSub = subId;
        String targetMandate = mandateId;

        // Auto-select latest active or pending SIP if not specified
        if ((targetSub == null || targetSub.isBlank()) || (targetMandate == null || targetMandate.isBlank())) {
            List<Sip> sips = sipRepository.findAll();
            if (!sips.isEmpty()) {
                Sip lastSip = sips.get(sips.size() - 1);
                if (targetSub == null || targetSub.isBlank()) targetSub = lastSip.getRazorpaySubscriptionId();
                if (targetMandate == null || targetMandate.isBlank()) targetMandate = lastSip.getUpiMandateId();
            } else {
                seedDemoSip();
                sips = sipRepository.findAll();
                Sip created = sips.get(sips.size() - 1);
                targetSub = created.getRazorpaySubscriptionId();
                targetMandate = created.getUpiMandateId();
            }
        }

        String eventId = "evt_sim_" + System.currentTimeMillis();
        String payloadJson;

        switch (eventType) {
            case "mandate.approved" -> payloadJson = """
                    {
                      "event": "mandate.approved",
                      "payload": {
                        "order": { "entity": { "id": "order_sim_001" } },
                        "payment": { "entity": { "id": "pay_sim_001", "vpa": "user@okhdfcbank" } },
                        "mandate": { "entity": { "id": "%s", "status": "approved" } }
                      }
                    }
                    """.formatted(targetMandate);

            case "payment.captured" -> payloadJson = """
                    {
                      "event": "payment.captured",
                      "payload": {
                        "subscription": { "entity": { "id": "%s" } },
                        "payment": { "entity": { "id": "pay_cap_%d", "amount": %.0f, "status": "captured" } }
                      }
                    }
                    """.formatted(targetSub, System.currentTimeMillis(), amount * 100);

            case "payment.failed" -> payloadJson = """
                    {
                      "event": "payment.failed",
                      "payload": {
                        "subscription": { "entity": { "id": "%s" } },
                        "payment": { "entity": { "id": "pay_fail_%d", "error_code": "BAD_REQUEST_ERROR", "error_description": "Insufficient funds in bank account" } }
                      }
                    }
                    """.formatted(targetSub, System.currentTimeMillis());

            case "subscription.cancelled" -> payloadJson = """
                    {
                      "event": "subscription.cancelled",
                      "payload": {
                        "subscription": { "entity": { "id": "%s", "status": "cancelled" } }
                      }
                    }
                    """.formatted(targetSub);

            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        }

        String signature = computeHmacSha256(payloadJson, razorpayProperties.getWebhookSecret());
        ResponseEntity<String> response = webhookController.handleRazorpay(
                payloadJson,
                signature
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("simulatedEvent", eventType);
        result.put("eventId", eventId);
        result.put("targetSubscription", targetSub);
        result.put("targetMandate", targetMandate);
        result.put("responseStatus", response.getStatusCode().value());
        result.put("responseBody", response.getBody());
        result.put("signature", signature);
        result.put("payload", payloadJson);
        return ResponseEntity.ok(result);
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
