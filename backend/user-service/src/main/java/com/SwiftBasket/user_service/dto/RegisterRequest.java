package com.SwiftBasket.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(name = "RegisterRequest", description = "User registration request")
public class RegisterRequest {
    @NotBlank(message="Email is required")
    @Email(message="Email should be valid")
    @Schema(description = "User's email address")
    private String email;
    
    @NotBlank(message="Password is required")
    @Size(min=8, message="Password must be at least 8 characters long")
    @Schema(description = "User's password (minimum 8 characters)")
    private String password;

    @NotBlank(message="Full name is required")
    @Schema(description = "User's full name")
    private String fullName;
}
