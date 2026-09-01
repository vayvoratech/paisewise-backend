package in.sapphirus.rupee.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Issues and validates JWTs. auth-service uses the issue methods; every service
 * uses {@link #parse} to validate incoming access tokens.
 */
@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(String userId, String phone, Map<String, Object> extraClaims) {
        return build(userId, phone, "access", props.getAccessTokenTtlMs(), extraClaims);
    }

    public String issueRefreshToken(String userId, String phone) {
        return build(userId, phone, "refresh", props.getRefreshTokenTtlMs(), Map.of());
    }

    private String build(String userId, String phone, String type, long ttlMs, Map<String, Object> extra) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(extra)
                .id(UUID.randomUUID().toString())
                .issuer(props.getIssuer())
                .subject(userId)
                .claim("phone", phone)
                .claim("type", type)
                .claim("nonce", UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMs)))
                .signWith(key)
                .compact();
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

    /** Parses and verifies a token; throws JwtException if invalid/expired. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }
}
