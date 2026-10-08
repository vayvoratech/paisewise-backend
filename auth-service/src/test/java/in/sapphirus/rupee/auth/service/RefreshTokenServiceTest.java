package in.sapphirus.rupee.auth.service;

import in.sapphirus.rupee.auth.domain.RefreshToken;
import in.sapphirus.rupee.auth.repo.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private final RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
    private final RefreshTokenService service = new RefreshTokenService(repo);

    @Test
    void createToken_savesAndReturnsToken() {
        UUID userId = UUID.randomUUID();
        String hash = "somehash";
        Instant expiry = Instant.now().plusSeconds(60);
        RefreshToken token = new RefreshToken(userId, hash, expiry);

        when(repo.save(any(RefreshToken.class))).thenReturn(token);

        RefreshToken result = service.createToken(userId, hash, expiry);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTokenHash()).isEqualTo(hash);
        verify(repo, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void findByTokenHash_callsRepository() {
        String hash = "targethash";
        RefreshToken token = new RefreshToken(UUID.randomUUID(), hash, Instant.now().plusSeconds(60));
        when(repo.findByTokenHash(hash)).thenReturn(Optional.of(token));

        Optional<RefreshToken> result = service.findByTokenHash(hash);

        assertThat(result).isPresent();
        assertThat(result.get().getTokenHash()).isEqualTo(hash);
        verify(repo, times(1)).findByTokenHash(hash);
    }

    @Test
    void validateToken_activeToken_returnsTrue() {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), "hash", Instant.now().plusSeconds(60));
        boolean result = service.validateToken(token);
        assertThat(result).isTrue();
    }

    @Test
    void validateToken_nullToken_returnsFalse() {
        boolean result = service.validateToken(null);
        assertThat(result).isFalse();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), "hash", Instant.now().minusSeconds(10));
        boolean result = service.validateToken(token);
        assertThat(result).isFalse();
    }

    @Test
    void validateToken_revokedToken_returnsFalse() {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), "hash", Instant.now().plusSeconds(60));
        token.revoke();
        boolean result = service.validateToken(token);
        assertThat(result).isFalse();
    }

    @Test
    void revokeRefreshToken_setsRevokedAndSaves() {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), "hash", Instant.now().plusSeconds(60));
        service.revokeRefreshToken(token);

        assertThat(token.isRevoked()).isTrue();
        verify(repo, times(1)).save(token);
    }

    @Test
    void revokeAllUserTokens_callsRepoRevokeAll() {
        UUID userId = UUID.randomUUID();
        doNothing().when(repo).revokeAllForUser(userId);

        service.revokeAllUserTokens(userId);

        verify(repo, times(1)).revokeAllForUser(userId);
    }
}
