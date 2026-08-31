package in.sapphirus.rupee.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration shared by every service.
 *
 * SECURITY: {@code secret} must be a strong, externally-provided value (env var
 * / secrets manager) of at least 256 bits — never hardcode it in source or
 * commit a real secret. The default below is for local dev only.
 */
@ConfigurationProperties(prefix = "rupee.jwt")
public class JwtProperties {

    /** HMAC signing secret (Base64 or raw). Override per environment. */
    private String secret = "local-dev-only-change-me-to-a-strong-256-bit-secret-value!!";

    /** Access-token lifetime in milliseconds (default 15 minutes). */
    private long accessTokenTtlMs = 15 * 60 * 1000L;

    /** Refresh-token lifetime in milliseconds (default 30 days). */
    private long refreshTokenTtlMs = 30L * 24 * 60 * 60 * 1000L;

    /** Token issuer claim. */
    private String issuer = "rupee-auth";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getAccessTokenTtlMs() { return accessTokenTtlMs; }
    public void setAccessTokenTtlMs(long v) { this.accessTokenTtlMs = v; }

    public long getRefreshTokenTtlMs() { return refreshTokenTtlMs; }
    public void setRefreshTokenTtlMs(long v) { this.refreshTokenTtlMs = v; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}
