package com.SwiftBasket.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LoginResponse", description = "User login response with JWT token")
public class LoginResponse {
    @Schema(description = "JWT Bearer token for authentication")
    private String token;

    @Schema(description = "Token expiration time in milliseconds")
    private Long expiresIn;

    @Schema(description = "Authenticated user ID")
    private Long userId;

    @Schema(description = "Authenticated user's email")
    private String email;
}
