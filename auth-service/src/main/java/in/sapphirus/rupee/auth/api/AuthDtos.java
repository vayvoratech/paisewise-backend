package in.sapphirus.rupee.auth.api;

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
            @NotBlank @Pattern(regexp = "\\d{10}", message = "phone must be 10 digits") String phone,
            @NotBlank String name,
            @NotBlank @Size(min = 4, max = 100) String password) {}

    public record LoginRequest(
            @NotBlank String phone,
            @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record UserView(String id, String name, String phone) {}

    public record Tokens(String accessToken, String refreshToken) {}

    public record AuthResponse(UserView user, Tokens tokens) {}
}
