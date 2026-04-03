package az.matcha.auth.controller;

import az.matcha.auth.dto.*;
import az.matcha.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Authentication", description = "Register, login, token refresh, and logout")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new account")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String deviceInfo = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.login(request, deviceInfo));
    }

    @Operation(summary = "Refresh access token using a valid refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "Revoke a refresh token (logout from one device)")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Revoke all refresh tokens (logout from all devices)")
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        authService.logoutAll(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get current authenticated user info")
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.getMe(UUID.fromString(jwt.getSubject())));
    }

    @Operation(summary = "Generate a 6-digit Telegram link code (sent to email in future sprint)")
    @PostMapping("/telegram/link-code")
    public ResponseEntity<MessageResponse> generateTelegramLinkCode(@AuthenticationPrincipal Jwt jwt) {
        String code = authService.generateTelegramLinkCode(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok(new MessageResponse("Verification code generated: " + code));
    }

    @Operation(summary = "Verify Telegram link code and bind Telegram chat ID")
    @PostMapping("/telegram/verify")
    public ResponseEntity<MessageResponse> verifyTelegramLink(@Valid @RequestBody TelegramLinkRequest request) {
        authService.verifyTelegramLink(request);
        return ResponseEntity.ok(new MessageResponse("Telegram account linked successfully"));
    }
}
