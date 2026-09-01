package in.sapphirus.rupee.auth.event;

import java.util.UUID;

public record AuditSecurityEvent(
        UUID userId,
        String action, // LOGIN, LOGOUT, OTP_SENT, OTP_VERIFIED, MPIN_SET, MPIN_CHANGED, ACCOUNT_LOCKED
        String result, // SUCCESS, FAILURE
        String failureReason,
        String ipAddress
) {}
