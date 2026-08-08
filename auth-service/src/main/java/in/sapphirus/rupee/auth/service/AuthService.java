package in.sapphirus.rupee.auth.service;

import in.sapphirus.rupee.auth.api.AuthDtos.*;
import in.sapphirus.rupee.auth.domain.OtpEntity;
import in.sapphirus.rupee.auth.domain.RefreshToken;
import in.sapphirus.rupee.auth.domain.User;
import in.sapphirus.rupee.auth.repo.OtpRepository;
import in.sapphirus.rupee.auth.repo.UserRepository;
import in.sapphirus.rupee.security.JwtProperties;
import in.sapphirus.rupee.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.context.ApplicationEventPublisher;
import in.sapphirus.rupee.auth.event.AuditSecurityEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenService refreshTokenService;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final JwtProperties jwtProps;
    private final JavaMailSender mailSender;
    private final OtpService otpService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(UserRepository users, RefreshTokenService refreshTokenService,
                       OtpRepository otpRepository, PasswordEncoder passwordEncoder,
                       JwtService jwt, JwtProperties jwtProps, JavaMailSender mailSender,
                       OtpService otpService, ApplicationEventPublisher eventPublisher) {
        this.users = users;
        this.refreshTokenService = refreshTokenService;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.jwtProps = jwtProps;
        this.mailSender = mailSender;
        this.otpService = otpService;
        this.eventPublisher = eventPublisher;
    }

    // --- CORE AUTHENTICATION ---

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByPhone(req.phone())) {
            throw AuthException.phoneTaken();
        }

        User user = new User(req.phone(), req.name(), req.email(), passwordEncoder.encode(req.password()));
        user = users.save(user);

        return issueFor(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        try {
            User user = users.findByPhone(req.phone())
                    .orElseThrow(() -> {
                        publishEvent(null, "LOGIN", "FAILURE", "USER_NOT_FOUND");
                        return AuthException.invalidCredentials();
                    });

            if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
                publishEvent(user.getId(), "LOGIN", "FAILURE", "WRONG_PASSWORD");
                throw AuthException.invalidCredentials();
            }
            AuthResponse response = issueFor(user);
            publishEvent(user.getId(), "LOGIN", "SUCCESS", null);
            return response;
        } catch (Exception e) {
            if (!(e instanceof AuthException)) {
                publishEvent(null, "LOGIN", "FAILURE", e.getMessage());
            }
            throw e;
        }
    }

    @Transactional
    public Tokens refresh(String presentedRefreshToken) {
        Claims claims = validateAndParseRefreshToken(presentedRefreshToken);
        String hash = sha256(presentedRefreshToken);

        RefreshToken stored = refreshTokenService.findByTokenHash(hash)
                .orElseThrow(AuthException::invalidRefresh);

        if (!refreshTokenService.validateToken(stored)) {
            throw AuthException.invalidRefresh();
        }

        refreshTokenService.revokeRefreshToken(stored);
        return mintTokens(UUID.fromString(claims.getSubject()), claims.get("phone", String.class));
    }

    // --- PASSWORD RESET FLOW ---

    @Transactional
    public void forgotPassword(String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> {
                    publishEvent(null, "OTP_SENT", "FAILURE", "USER_NOT_FOUND");
                    return AuthException.userNotFound();
                });
        try {
            otpService.sendOtp(user.getPhone());
            publishEvent(user.getId(), "OTP_SENT", "SUCCESS", null);
        } catch (Exception e) {
            publishEvent(user.getId(), "OTP_SENT", "FAILURE", e.getMessage());
            throw e;
        }
    }

    public boolean verifyOtp(String email, String otp) {
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            publishEvent(null, "OTP_VERIFIED", "FAILURE", "USER_NOT_FOUND");
            return false;
        }
        try {
            boolean isValid = otpService.verifyOtp(user.getPhone(), otp);
            if (isValid) {
                publishEvent(user.getId(), "OTP_VERIFIED", "SUCCESS", null);
            } else {
                publishEvent(user.getId(), "OTP_VERIFIED", "FAILURE", "INVALID_OTP");
            }
            return isValid;
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("locked")) {
                publishEvent(user.getId(), "ACCOUNT_LOCKED", "SUCCESS", "TOO_MANY_FAILED_ATTEMPTS");
            } else {
                publishEvent(user.getId(), "OTP_VERIFIED", "FAILURE", e.getMessage());
            }
            throw e;
        }
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        users.findByEmail(email).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            users.save(user);
        });
    }

    public void sendOtpForPhone(String phone) {
        User user = users.findByPhone(phone).orElse(null);
        UUID userId = user != null ? user.getId() : null;
        try {
            otpService.sendOtp(phone);
            publishEvent(userId, "OTP_SENT", "SUCCESS", null);
        } catch (Exception e) {
            publishEvent(userId, "OTP_SENT", "FAILURE", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void setMpin(String email, String mpin) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> {
                    publishEvent(null, "MPIN_SET", "FAILURE", "USER_NOT_FOUND");
                    return AuthException.userNotFound();
                });
        try {
            String action = (user.getMpinHash() == null) ? "MPIN_SET" : "MPIN_CHANGED";
            user.setMpinHash(passwordEncoder.encode(mpin));
            users.save(user);
            publishEvent(user.getId(), action, "SUCCESS", null);
        } catch (Exception e) {
            publishEvent(user.getId(), "MPIN_SET", "FAILURE", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public AuthResponse loginWithMpin(MpinLoginRequest req) {
        try {
            otpService.checkMpinLockout(req.phone());
        } catch (IllegalStateException e) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "ACCOUNT_LOCKED", e.getMessage());
        }

        User user = users.findByPhone(req.phone())
                .orElseThrow(AuthException::invalidCredentials);

        if (user.getMpinHash() == null || !passwordEncoder.matches(req.mpin(), user.getMpinHash())) {
            try {
                otpService.trackMpinFailure(req.phone());
            } catch (IllegalStateException e) {
                publishEvent(user.getId(), "ACCOUNT_LOCKED", "SUCCESS", "TOO_MANY_FAILED_MPIN_ATTEMPTS");
                throw new AuthException(HttpStatus.UNAUTHORIZED, "ACCOUNT_LOCKED", e.getMessage());
            }
            publishEvent(user.getId(), "LOGIN", "FAILURE", "WRONG_MPIN");
            throw AuthException.invalidCredentials();
        }

        otpService.resetMpinAttempts(req.phone());
        AuthResponse response = issueFor(user);
        publishEvent(user.getId(), "LOGIN", "SUCCESS", null);
        return response;
    }

    @Transactional
    public void logout(String presentedRefreshToken) {
        String hash = sha256(presentedRefreshToken);
        refreshTokenService.findByTokenHash(hash).ifPresent(stored -> {
            refreshTokenService.revokeRefreshToken(stored);
            publishEvent(stored.getUserId(), "LOGOUT", "SUCCESS", null);
        });
    }

    // --- HELPER METHODS ---

    private AuthResponse issueFor(User user) {
        Tokens tokens = mintTokens(user.getId(), user.getPhone());
        UserView view = new UserView(user.getId().toString(), user.getName(), user.getPhone());
        return new AuthResponse(view, tokens);
    }

    private Tokens mintTokens(UUID userId, String phone) {
        String access = jwt.issueAccessToken(userId.toString(), phone, Map.of());
        String refresh = jwt.issueRefreshToken(userId.toString(), phone);
        Instant expiry = Instant.now().plusMillis(jwtProps.getRefreshTokenTtlMs());
        refreshTokenService.createToken(userId, sha256(refresh), expiry);
        return new Tokens(access, refresh);
    }

    private void sendOtpEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset OTP");
        message.setText("Your OTP is: " + code + ". Valid for 5 minutes.");
        mailSender.send(message);
    }

    private Claims validateAndParseRefreshToken(String token) {
        try {
            Claims claims = jwt.parse(token);
            if (!jwt.isRefreshToken(claims)) {
                throw AuthException.invalidRefresh();
            }
            return claims;
        } catch (JwtException e) {
            throw AuthException.invalidRefresh();
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String getClientIp() {
        try {
            var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttributes) {
                var request = servletAttributes.getRequest();
                String xff = request.getHeader("X-Forwarded-For");
                if (xff != null && !xff.isBlank()) {
                    return xff.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // fallback
        }
        return "127.0.0.1";
    }

    private void publishEvent(UUID userId, String action, String result, String failureReason) {
        try {
            String ip = getClientIp();
            eventPublisher.publishEvent(new AuditSecurityEvent(userId, action, result, failureReason, ip));
        } catch (Exception e) {
            // log fallback
        }
    }
}