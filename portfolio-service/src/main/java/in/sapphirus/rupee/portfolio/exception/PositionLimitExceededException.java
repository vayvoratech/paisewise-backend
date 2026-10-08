package in.sapphirus.rupee.portfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PositionLimitExceededException extends RiskCheckException {
    public PositionLimitExceededException(String message) {
        super("POSITION_LIMIT_EXCEEDED", message);
    }
}
