package in.sapphirus.rupee.auth.service;

import in.sapphirus.rupee.auth.domain.RefreshToken;
import in.sapphirus.rupee.auth.repo.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;

    public RefreshTokenService(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public RefreshToken createToken(UUID userId, String hash, Instant expiry) {
        return repo.save(new RefreshToken(userId, hash, expiry));
    }

    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repo.findByTokenHash(tokenHash);
    }

    public boolean validateToken(RefreshToken token) {
        return token != null && token.isActive();
    }

    @Transactional
    public void revokeRefreshToken(RefreshToken token) {
        token.revoke();
        repo.save(token);
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        repo.revokeAllForUser(userId);
    }
}
