package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.domain.Sip;
import in.sapphirus.rupee.portfolio.service.SipService;
import in.sapphirus.rupee.security.CurrentUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST API for SIP lifecycle management.
 *
 * All endpoints are JWT-protected (handled by the shared security filter chain).
 * Ownership is verified in {@link SipService} — a user can only operate on their own SIPs.
 *
 * <pre>
 * GET    /sip/me         List all SIPs for the authenticated user
 * GET    /sip/{id}       Get a single SIP
 * POST   /sip/{id}/pause Pause an active SIP
 * POST   /sip/{id}/resume Resume a paused SIP
 * DELETE /sip/{id}       Cancel a SIP
 * </pre>
 */
@RestController
@RequestMapping("/sip")
@Validated
public class SipController {

    private final SipService sipService;

    public SipController(SipService sipService) {
        this.sipService = sipService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Response DTOs
    // ─────────────────────────────────────────────────────────────────────────

    public record SipResponse(
            UUID id,
            UUID userId,
            String schemeCode,
            UUID goalId,
            String frequency,
            double amount,
            int debitDay,
            String status,
            String upiMandateId,
            String upiMandateStatus,
            String razorpaySubscriptionId,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate nextDebitDate,
            int installmentsPlanned,
            int installmentsDone,
            int installmentsFailed,
            double totalInvested,
            double totalUnits,
            Instant pausedAt,
            String pausedReason,
            Instant cancelledAt,
            String cancelledReason,
            Instant createdAt,
            Instant updatedAt
    ) {
        /** Factory method to map domain Sip → SipResponse. */
        static SipResponse from(Sip s) {
            return new SipResponse(
                    s.getId(), s.getUserId(), s.getSchemeCode(), s.getGoalId(),
                    s.getFrequency(), s.getAmount(), s.getDebitDay(), s.getStatus(),
                    s.getUpiMandateId(), s.getUpiMandateStatus(), s.getRazorpaySubscriptionId(),
                    s.getStartDate(), s.getEndDate(), s.getNextDebitDate(),
                    s.getInstallmentsPlanned(), s.getInstallmentsDone(), s.getInstallmentsFailed(),
                    s.getTotalInvested(), s.getTotalUnits(),
                    s.getPausedAt(), s.getPausedReason(),
                    s.getCancelledAt(), s.getCancelledReason(),
                    s.getCreatedAt(), s.getUpdatedAt()
            );
        }
    }

    /** Request body for pause / cancel. */
    public record ReasonRequest(
            @NotBlank(message = "reason is required")
            @Size(max = 300, message = "reason must be ≤ 300 characters")
            String reason
    ) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * List all SIPs for the authenticated user.
     *
     * <p>GET /sip/me</p>
     */
    @GetMapping("/me")
    public List<SipResponse> listMySips() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return sipService.listMySips(userId)
                .stream()
                .map(SipResponse::from)
                .toList();
    }

    /**
     * Get a single SIP by ID.
     *
     * <p>GET /sip/{id}</p>
     * @return 200 with SipResponse, or 404 if not found / not owned by caller
     */
    @GetMapping("/{id}")
    public SipResponse getSip(@PathVariable UUID id) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return SipResponse.from(sipService.getSip(id, userId));
    }

    /**
     * Pause an active SIP.
     *
     * <p>POST /sip/{id}/pause</p>
     * Body: {@code {"reason": "Going on vacation"}}
     *
     * @return 200 with updated SipResponse
     */
    @PostMapping("/{id}/pause")
    public SipResponse pauseSip(@PathVariable UUID id,
                                 @RequestBody @Validated ReasonRequest req) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return SipResponse.from(sipService.pauseSip(id, userId, req.reason()));
    }

    /**
     * Resume a paused SIP.
     *
     * <p>POST /sip/{id}/resume</p>
     * No request body required.
     *
     * @return 200 with updated SipResponse
     */
    @PostMapping("/{id}/resume")
    public SipResponse resumeSip(@PathVariable UUID id) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return SipResponse.from(sipService.resumeSip(id, userId));
    }

    /**
     * Cancel a SIP (irreversible).
     *
     * <p>DELETE /sip/{id}</p>
     * Body: {@code {"reason": "No longer needed"}}
     *
     * @return 200 with updated SipResponse (status = CANCELLED)
     */
    @DeleteMapping("/{id}")
    public SipResponse cancelSip(@PathVariable UUID id,
                                  @RequestBody @Validated ReasonRequest req) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return SipResponse.from(sipService.cancelSip(id, userId, req.reason()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exception handlers (translate domain exceptions to HTTP status codes)
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(SipService.SipNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorBody> handleNotFound(SipService.SipNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody("SIP_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(SipService.SipAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorBody> handleForbidden(SipService.SipAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorBody("SIP_ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(SipService.SipStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorBody> handleState(SipService.SipStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody("SIP_INVALID_STATE", ex.getMessage()));
    }

    @ExceptionHandler(SipService.SipOperationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ResponseEntity<ErrorBody> handleOperation(SipService.SipOperationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorBody("RAZORPAY_ERROR", ex.getMessage()));
    }

    public record ErrorBody(String code, String message) {}
}
