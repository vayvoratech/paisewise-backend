package in.sapphirus.rupee.auth.service;

import org.springframework.http.HttpStatus;

/** Domain error carrying an HTTP status + machine-readable code for the client. */
public class AuthException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public AuthException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid phone or password.");
    }

    public static AuthException phoneTaken() {
        return new AuthException(HttpStatus.CONFLICT, "PHONE_TAKEN", "An account with this phone already exists.");
    }

    public static AuthException invalidRefresh() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH", "Refresh token is invalid or expired.");
    }

    public static AuthException userNotFound() {
        return new AuthException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "No account found with that email.");
    }
}