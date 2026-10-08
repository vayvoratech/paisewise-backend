package in.sapphirus.rupee.portfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class OrderVelocityExceededException extends RiskCheckException {
    public OrderVelocityExceededException(String message) {
        super("ORDER_VELOCITY_EXCEEDED", message);
    }
}
