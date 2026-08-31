package in.sapphirus.rupee.auth.service;

import in.sapphirus.rupee.auth.api.AuthDtos.*;
import in.sapphirus.rupee.auth.domain.OtpEntity;
import in.sapphirus.rupee.auth.domain.RefreshToken;
import in.sapphirus.rupee.auth.domain.User;
import in.sapphirus.rupee.auth.repo.OtpRepository;
import in.sapphirus.rupee.auth.repo.RefreshTokenRepository;
import in.sapphirus.rupee.auth.repo.UserRepository;
import in.sapphirus.rupee.security.JwtProperties;
import in.sapphirus.rupee.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final RefreshTokenRepository refreshTokens;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final JwtProperties jwtProps;
    private final JavaMailSender mailSender;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       OtpRepository otpRepository, PasswordEncoder passwordEncoder,
                       JwtService jwt, JwtProperties jwtProps, JavaMailSender mailSender) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.jwtProps = jwtProps;
        this.mailSender = mailSender;
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
        User user = users.findByPhone(req.phone())
                .orElseThrow(AuthException::invalidCredentials);

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            // Same message as "user not found" above so we don't leak which part
            // of the credentials was wrong.
            throw AuthException.invalidCredentials();
        }
        return issueFor(user);
    }

    @Transactional
    public Tokens refresh(String presentedRefreshToken) {
        Claims claims = validateAndParseRefreshToken(presentedRefreshToken);
        String hash = sha256(presentedRefreshToken);

        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(AuthException::invalidRefresh);

        if (!stored.isActive()) {
            throw AuthException.invalidRefresh();
        }

        stored.revoke();
        return mintTokens(UUID.fromString(claims.getSubject()), claims.get("phone", String.class));
    }

    // --- PASSWORD RESET FLOW ---

    @Transactional
    public void forgotPassword(String email) {
        try {
            System.out.println("DEBUG: Looking for email: " + email);

            users.findByEmail(email).ifPresentOrElse(user -> {
                System.out.println("DEBUG: User found, generating OTP...");

                otpRepository.deleteByEmail(email);
                String code = String.format("%06d", new Random().nextInt(999999));

                OtpEntity otp = new OtpEntity();
                otp.setEmail(email);
                otp.setOtp(passwordEncoder.encode(code));
                otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
                otpRepository.save(otp);

                sendOtpEmail(email, code);
                System.out.println("DEBUG: Email sent successfully.");

            }, () -> {
                System.out.println("DEBUG: User not found in DB.");
                throw AuthException.userNotFound();
            });

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR in forgotPassword: " + e.getMessage());
            e.printStackTrace();
            throw new AuthException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Failed to send OTP. Please try again later.");
        }
    }

    public boolean verifyOtp(String email, String otp) {
        return otpRepository.findByEmail(email)
                .map(o -> !o.isVerified() &&
                        o.getExpiresAt().isAfter(LocalDateTime.now()) &&
                        passwordEncoder.matches(otp, o.getOtp()))
                .orElse(false);
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        users.findByEmail(email).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            users.save(user);
            otpRepository.deleteByEmail(email);
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
        refreshTokens.save(new RefreshToken(userId, sha256(refresh), expiry));
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
}