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
@Schema(name = "UserEmailResponse", description = "Internal claim-check response: a user's current email")
public class UserEmailResponse {

    @Schema(description = "User's unique ID")
    private Long userId;

    @Schema(description = "User's current email address")
    private String email;
}
