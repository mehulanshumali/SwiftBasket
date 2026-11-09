package com.SwiftBasket.notification_service.event;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserRegisteredEvent", description = "Kafka event from user-service; triggers the welcome email")
public class UserRegisteredEvent {

    @Schema(description = "New user's ID")
    private Long userId;

    @Schema(description = "Registered email (PII exception: emitted once by the source of truth)")
    private String email;

    @Schema(description = "Display name for the welcome email greeting")
    private String fullName;

    @Schema(description = "Registration timestamp")
    private LocalDateTime registeredAt;
}
