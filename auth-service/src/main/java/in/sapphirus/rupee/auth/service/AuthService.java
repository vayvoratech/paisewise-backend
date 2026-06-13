package in.sapphirus.rupee.auth.service;

import in.sapphirus.rupee.auth.api.AuthDtos.*;
import in.sapphirus.rupee.auth.domain.RefreshToken;
import in.sapphirus.rupee.auth.domain.User;
import in.sapphirus.rupee.auth.repo.RefreshTokenRepository;
import in.sapphirus.rupee.auth.repo.UserRepository;
import in.sapphirus.rupee.security.JwtProperties;
import in.sapphirus.rupee.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Core authentication logic: register, login, and refresh-token rotation.
 *
 * SECURITY notes:
 *  - Passwords are stored as BCrypt hashes only.
 *  - Login returns the SAME generic error whether the phone is unknown or the
 *    password is wrong, to avoid user enumeration.
 *  - Refresh tokens are stored hashed; on each refresh the old token is revoked
 *    and a new pair issued (rotation), limiting the blast radius of a leak.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final JwtProperties jwtProps;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, JwtService jwt, JwtProperties jwtProps) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.jwtProps = jwtProps;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByPhone(req.phone())) {
            throw AuthException.phoneTaken();
        }
        User user = users.save(new User(req.phone(), req.name(), passwordEncoder.encode(req.password())));
        return issueFor(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = users.findByPhone(req.phone()).orElseThrow(AuthException::invalidCredentials);
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw AuthException.invalidCredentials();
        }
        return issueFor(user);
    }

    @Transactional
    public Tokens refresh(String presentedRefreshToken) {
        Claims claims;
        try {
            claims = jwt.parse(presentedRefreshToken);
        } catch (JwtException e) {
            throw AuthException.invalidRefresh();
        }
        if (!jwt.isRefreshToken(claims)) {
            throw AuthException.invalidRefresh();
        }

        String hash = sha256(presentedRefreshToken);
        RefreshToken stored = refreshTokens.findByTokenHash(hash).orElseThrow(AuthException::invalidRefresh);
        if (!stored.isActive()) {
            throw AuthException.invalidRefresh();
        }

        // Rotate: revoke the presented token, mint a fresh pair.
        stored.revoke();
        UUID userId = UUID.fromString(claims.getSubject());
        String phone = claims.get("phone", String.class);
        return mintTokens(userId, phone);
    }

    private AuthResponse issueFor(User user) {
        Tokens tokens = mintTokens(user.getId(), user.getPhone());
        UserView view = new UserView(user.getId().toString(), user.getName(), user.getPhone());
        return new AuthResponse(view, tokens);
    }

    private Tokens mintTokens(UUID userId, String phone) {
        String access = jwt.issueAccessToken(userId.toString(), phone, Map.of());
        String refresh = jwt.issueRefreshToken(userId.toString(), phone);
        Instant expiry = Instant.now().plusMillis(jwtProps.getRefreshTokenTtlMs());
        refreshTokens.save(new RefreshToken(userId, sha256(refresh), expiry));
        return new Tokens(access, refresh);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
