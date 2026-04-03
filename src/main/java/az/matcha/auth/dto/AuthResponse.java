package az.matcha.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing JWT access token and opaque refresh token")
public record AuthResponse(

        @Schema(description = "RS256 signed JWT access token, valid for 15 minutes")
        String accessToken,

        @Schema(description = "Opaque refresh token, valid for 7 days")
        String refreshToken,

        @Schema(description = "Token type", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifetime in seconds", example = "900")
        long expiresIn
) {
}
