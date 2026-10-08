package in.sapphirus.rupee.portfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientMarginException extends RiskCheckException {
    public InsufficientMarginException(String message) {
        super("INSUFFICIENT_MARGIN", message);
    }
}
