package in.sapphirus.rupee.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request/response payloads. Shapes match the mobile client's auth.types.ts:
 * login → { user, tokens }, refresh → { accessToken, refreshToken }.
 */
public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "\\d{10}") String phone,
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 4, max = 100) String password,
            @NotBlank String confirmPassword
    ) {}

    public record LoginRequest(
            @NotBlank String identifier,
            @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record UserView(String id, String name, String phone, String email, boolean hasMpin) {}

    public record Tokens(String accessToken, String refreshToken) {}

    public record AuthResponse(UserView user, Tokens tokens) {}

    // --- Forgot Password DTOs ---

    public record ForgotPasswordRequest(
            @NotBlank @Email String email
    ) {}

    public record VerifyOtpRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 6) String otp
    ) {}

    public record ResetPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 4, max = 100) String newPassword
    ) {}

    public record SendOtpRequest(
            @NotBlank @Pattern(regexp = "\\d{10}") String phone
    ) {}

    public record SetMpinRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 4, max = 6) String mpin
    ) {}

    public record MpinLoginRequest(
            @NotBlank @Pattern(regexp = "\\d{10}") String phone,
            @NotBlank @Size(min = 4, max = 6) String mpin
    ) {}
}