package az.matcha.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Token refresh request")
public record RefreshRequest(

        @Schema(description = "Opaque refresh token received at login")
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
