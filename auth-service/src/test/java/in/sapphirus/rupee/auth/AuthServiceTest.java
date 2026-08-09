package in.sapphirus.rupee.auth;

import in.sapphirus.rupee.auth.api.AuthDtos.*;
import in.sapphirus.rupee.auth.service.AuthException;
import in.sapphirus.rupee.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** End-to-end auth flow against in-memory H2 (local profile). */
@SpringBootTest
@ActiveProfiles("local")
class AuthServiceTest {

    @Autowired
    AuthService authService;

    @Test
    void register_then_login_then_refresh() {
        var reg = authService.register(new RegisterRequest("9876543210", "Rahul", "secret1", "rahul@example.com", "USER"));
        assertThat(reg.user().name()).isEqualTo("Rahul");
        assertThat(reg.tokens().accessToken()).isNotBlank();
        assertThat(reg.tokens().refreshToken()).isNotBlank();

        var login = authService.login(new LoginRequest("9876543210", "secret1"));
        assertThat(login.user().phone()).isEqualTo("9876543210");

        var refreshed = authService.refresh(login.tokens().refreshToken());
        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotEqualTo(login.tokens().refreshToken());
    }

    @Test
    void login_with_wrong_password_fails() {
        authService.register(new RegisterRequest("9000000001", "Test", "rightpass", "test1@example.com", "USER"));
        assertThatThrownBy(() -> authService.login(new LoginRequest("9000000001", "wrongpass")))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void rotated_refresh_token_cannot_be_reused() {
        var reg = authService.register(new RegisterRequest("9000000002", "Test", "rightpass", "test2@example.com", "USER"));
        String original = reg.tokens().refreshToken();
        authService.refresh(original); // rotates → original now revoked
        assertThatThrownBy(() -> authService.refresh(original))
                .isInstanceOf(AuthException.class);
    }
}