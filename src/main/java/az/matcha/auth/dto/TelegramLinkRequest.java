package az.matcha.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Telegram account link verification request")
public record TelegramLinkRequest(

        @Schema(description = "6-digit one-time verification code sent to the user's email", example = "482931")
        @NotBlank(message = "Code is required")
        @Size(min = 6, max = 6, message = "Code must be exactly 6 digits")
        @Pattern(regexp = "\\d{6}", message = "Code must contain only digits")
        String code,

        @Schema(description = "Telegram chat ID of the user", example = "123456789")
        @NotBlank(message = "Telegram chat ID is required")
        String telegramChatId
) {
}
