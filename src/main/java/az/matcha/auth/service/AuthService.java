package az.matcha.auth.service;

import az.matcha.auth.domain.RefreshToken;
import az.matcha.auth.domain.User;
import az.matcha.auth.dto.*;
import az.matcha.auth.exception.AuthException;
import az.matcha.auth.repository.RefreshTokenRepository;
import az.matcha.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiry}")
    private Duration refreshTokenExpiry;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthException("Email is already registered", HttpStatus.CONFLICT);
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();
        userRepository.save(user);
        log.info("New user registered: userId={} role={}", user.getId(), user.getRole());
        return issueTokens(user, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo) {
        if (loginAttemptService.isBlocked(request.email())) {
            throw new AuthException("Too many failed login attempts. Try again after 15 minutes.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.loginFailed(request.email());
            throw new AuthException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }
        loginAttemptService.loginSucceeded(request.email());
        MDC.put("userId", user.getId().toString());
        log.info("User logged in: userId={}", user.getId());
        return issueTokens(user, deviceInfo);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = jwtService.hashToken(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (stored.isRevoked()) {
            throw new AuthException("Refresh token has been revoked", HttpStatus.UNAUTHORIZED);
        }
        if (Instant.now().isAfter(stored.getExpiresAt())) {
            throw new AuthException("Refresh token has expired", HttpStatus.UNAUTHORIZED);
        }
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser(), stored.getDeviceInfo());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = jwtService.hashToken(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("All sessions revoked for userId={}", userId);
    }

    public UserInfoResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));
        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getTelegramChatId() != null,
                user.getCreatedAt()
        );
    }

    @Transactional
    public String generateTelegramLinkCode(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        user.setTelegramLinkCode(code);
        user.setTelegramLinkCodeExpiresAt(Instant.now().plusSeconds(900)); // 15 min
        userRepository.save(user);
        log.info("Telegram link code generated for userId={}", userId);
        return code;
    }

    @Transactional
    public void verifyTelegramLink(TelegramLinkRequest request) {
        User user = userRepository.findByTelegramLinkCode(request.code())
                .filter(u -> u.getTelegramLinkCodeExpiresAt() != null
                        && Instant.now().isBefore(u.getTelegramLinkCodeExpiresAt()))
                .orElseThrow(() -> new AuthException("Invalid or expired verification code",
                        HttpStatus.UNAUTHORIZED));

        user.setTelegramChatId(request.telegramChatId());
        user.setTelegramLinkCode(null);
        user.setTelegramLinkCodeExpiresAt(null);
        userRepository.save(user);
        log.info("Telegram linked for userId={}", user.getId());
    }

    private AuthResponse issueTokens(User user, String deviceInfo) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(rawRefreshToken))
                .expiresAt(Instant.now().plus(refreshTokenExpiry))
                .deviceInfo(deviceInfo)
                .build();
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                jwtService.getAccessTokenExpiry().getSeconds()
        );
    }

    @Scheduled(cron = "0 0 3 * * *") // 3 AM daily
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
        log.info("Expired and revoked refresh tokens cleaned up");
    }
}
