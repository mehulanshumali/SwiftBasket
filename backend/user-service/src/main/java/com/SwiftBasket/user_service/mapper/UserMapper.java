// UserMapper.java
package com.SwiftBasket.user_service.mapper;

import org.springframework.stereotype.Component;

import com.SwiftBasket.user_service.dto.RegisterRequest;
import com.SwiftBasket.user_service.dto.UserProfileResponse;
import com.SwiftBasket.user_service.entity.User;

@Component
public class UserMapper {
    
    public UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public User toEntity(RegisterRequest request) {
        return User.builder()
                .email(request.getEmail())
                .build();
    }
}