package in.sapphirus.rupee.portfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RiskCheckException extends RuntimeException {

    private final String reasonCode;

    public RiskCheckException(String message) {
        super(message);
        this.reasonCode = "RISK_CHECK_FAILED";
    }

    public RiskCheckException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String getReasonCode() {
        return reasonCode;
    }
}
