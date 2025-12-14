package com.SwiftBasket.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ResetPasswordRequest", description = "Password reset completion request")
public class ResetPasswordRequest {
    @NotBlank(message = "Token is required")
    @Schema(description = "Reset token received in email", example = "reset-token-xxx-yyy-zzz")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password should be at least 8 characters")
    @Schema(description = "New password (minimum 8 characters)", example = "NewSecurePass456")
    private String newPassword;
}
