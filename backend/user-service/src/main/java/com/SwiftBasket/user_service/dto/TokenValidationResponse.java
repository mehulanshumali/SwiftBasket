package com.SwiftBasket.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 
public class TokenValidationResponse {
    private Long userId;
    private String email;
    private String role;
}
