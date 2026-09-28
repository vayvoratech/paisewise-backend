package in.sapphirus.rupee.portfolio.config;

import in.sapphirus.rupee.security.JwtAuthenticationFilter;
import in.sapphirus.rupee.security.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Portfolio-service security configuration.
 *
 * Overrides the default {@code ResourceServerSecurityConfig} (via
 * {@code @ConditionalOnMissingBean}) to add one extra public path:
 * <ul>
 *   <li>{@code /webhooks/**} — Razorpay webhook callbacks; authenticated via
 *       HMAC-SHA256 signature, not JWT</li>
 * </ul>
 *
 * All other requests still require a valid JWT access token.
 */
@Configuration
@EnableConfigurationProperties({RazorpayProperties.class, BseStarMfProperties.class})
public class PortfolioSecurityConfig {

    /**
     * Defining this bean prevents {@code ResourceServerSecurityConfig#securityFilterChain}
     * from being registered (it is annotated {@code @ConditionalOnMissingBean}).
     */
    @Bean
    public SecurityFilterChain portfolioSecurityFilterChain(HttpSecurity http,
                                                             JwtService jwtService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Standard open endpoints
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        // Static preview UI and interactive test helper endpoints
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/preview.html",
                                "/static/**",
                                "/favicon.ico",
                                "/api/preview/**").permitAll()
                        // Razorpay webhook — authenticated by HMAC, NOT by JWT
                        .requestMatchers("/webhooks/**", "/portfolio/webhooks/**").permitAll()
                        // Everything else requires JWT
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
