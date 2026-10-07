package in.sapphirus.rupee.portfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class KycNotVerifiedException extends RiskCheckException {
    public KycNotVerifiedException(String message) {
        super("KYC_NOT_VERIFIED", message);
    }
}
