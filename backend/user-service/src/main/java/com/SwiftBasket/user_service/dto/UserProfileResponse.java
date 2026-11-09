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
    @Schema(description = "User's unique ID")
    private Long userId;
    
    @Schema(description = "User's email address")
    private String email;
    
    @Schema(description = "User's full name")
    private String fullName;
    
    @Schema(description = "User's phone number")
    private String phoneNumber;
    
    @Schema(description = "User's address")
    private String address;

    @Schema(description = "User's role")
    private String role;
    
    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last profile update timestamp")
    private LocalDateTime updatedAt;
}
