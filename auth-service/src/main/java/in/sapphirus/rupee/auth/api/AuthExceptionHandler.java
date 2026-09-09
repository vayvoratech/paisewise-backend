package in.sapphirus.rupee.auth.api;

import in.sapphirus.rupee.auth.service.AuthException;
import in.sapphirus.rupee.web.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handle(AuthException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                .body(new ApiError("ACCOUNT_EXISTS", "An account with this phone number or email address already exists. Please log in."));
    }
}
