package az.matcha.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Logout request — revokes the supplied refresh token")
public record LogoutRequest(

        @Schema(description = "Refresh token to revoke")
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
