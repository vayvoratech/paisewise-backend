package in.sapphirus.rupee.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final String secret;
    private final String issuer;
    private SecretKey key;

    public JwtValidationGatewayFilterFactory(
            @Value("${rupee.jwt.secret:rupee_secret_key_needs_to_be_long_and_secure_32_bytes}") String secret,
            @Value("${rupee.jwt.issuer:rupee-auth}") String issuer) {
        super(Object.class);
        this.secret = secret;
        this.issuer = issuer;
    }

    private SecretKey getSigningKey() {
        if (this.key == null) {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        return this.key;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            
            // Allow public auth endpoints without token check
            if (path.startsWith("/auth/login") || path.startsWith("/auth/register") 
                    || path.startsWith("/auth/verify-otp") || path.startsWith("/auth/forgot-password") 
                    || path.startsWith("/auth/reset-password") || path.startsWith("/auth/send-otp")
                    || path.startsWith("/auth/refresh") || path.startsWith("/auth/refresh-token")
                    || path.startsWith("/auth/logout") || path.startsWith("/auth/set-mpin")) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(getSigningKey())
                        .requireIssuer(issuer)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                
                // Inject the user ID (subject) into the request headers for downstream consumption (Task 18)
                String userId = claims.getSubject();
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (JwtException e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }
}
