package az.matcha.auth.dto;

import az.matcha.auth.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Authenticated user information")
public record UserInfoResponse(

        @Schema(description = "User ID")
        UUID id,

        @Schema(description = "User email")
        String email,

        @Schema(description = "Account role")
        Role role,

        @Schema(description = "Whether a Telegram account is linked")
        boolean telegramLinked,

        @Schema(description = "Account creation timestamp")
        Instant createdAt
) {
}
