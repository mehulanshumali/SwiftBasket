package com.SwiftBasket.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserProfileResponse", description = "User profile information")
public class UserProfileResponse {
    @Schema(description = "User's unique ID", example = "1")
    private Long userId;
    
    @Schema(description = "User's email address", example = "user@example.com")
    private String email;
    
    @Schema(description = "User's full name", example = "John Doe")
    private String fullName;
    
    @Schema(description = "User's phone number", example = "+1234567890")
    private String phoneNumber;
    
    @Schema(description = "User's address", example = "123 Main St, City, State 12345")
    private String address;

    @Schema(description = "User's role", example = "USER")
    private String role;
    
    @Schema(description = "Account creation timestamp", example = "2025-12-14T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last profile update timestamp", example = "2025-12-14T10:30:00")
    private LocalDateTime updatedAt;
}
