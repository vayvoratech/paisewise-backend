package in.sapphirus.rupee.portfolio.api;

import in.sapphirus.rupee.portfolio.exception.KycNotVerifiedException;
import in.sapphirus.rupee.portfolio.exception.OrderVelocityExceededException;
import in.sapphirus.rupee.portfolio.exception.RiskCheckException;
import in.sapphirus.rupee.web.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PortfolioExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        String code = ex.getStatusCode().toString();
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(new ApiError(code, reason));
    }

    @ExceptionHandler(KycNotVerifiedException.class)
    public ResponseEntity<ApiError> handleKyc(KycNotVerifiedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("KYC_NOT_VERIFIED", ex.getMessage()));
    }

    @ExceptionHandler(OrderVelocityExceededException.class)
    public ResponseEntity<ApiError> handleVelocity(OrderVelocityExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiError("ORDER_VELOCITY_EXCEEDED", ex.getMessage()));
    }

    @ExceptionHandler(RiskCheckException.class)
    public ResponseEntity<ApiError> handleRisk(RiskCheckException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getReasonCode(), ex.getMessage()));
    }
}
