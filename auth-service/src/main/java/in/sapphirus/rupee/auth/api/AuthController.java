package in.sapphirus.rupee.auth.api;

import in.sapphirus.rupee.auth.api.AuthDtos.*;
import in.sapphirus.rupee.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public auth endpoints. Routed via the gateway at /auth/**. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return new ResponseEntity<>(authService.register(req), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping({"/refresh", "/refresh-token"})
    public ResponseEntity<Tokens> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req.refreshToken()));
    }

    // --- Forgot Password Endpoints ---

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok("If the email exists, an OTP has been sent.");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean isValid = authService.verifyOtp(request.email(), request.otp());
        if (isValid) {
            return ResponseEntity.ok("OTP verified successfully.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired OTP.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.email(), request.newPassword());
        return ResponseEntity.ok("Password updated successfully.");
    }

    // --- MPIN and Logout Endpoints ---

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtpForPhone(request.phone());
        return ResponseEntity.ok("OTP sent successfully via SMS.");
    }

    @PostMapping("/set-mpin")
    public ResponseEntity<String> setMpin(@Valid @RequestBody SetMpinRequest request) {
        authService.setMpin(request.email(), request.mpin());
        return ResponseEntity.ok("MPIN configured successfully.");
    }

    @PostMapping("/login/mpin")
    public ResponseEntity<AuthResponse> loginMpin(@Valid @RequestBody MpinLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithMpin(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok("Logged out successfully.");
    }
}