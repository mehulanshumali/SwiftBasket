package com.SwiftBasket.user_service.service;

import com.SwiftBasket.user_service.dto.LoginRequest;
import com.SwiftBasket.user_service.dto.LoginResponse;
import com.SwiftBasket.user_service.dto.RegisterRequest;
import com.SwiftBasket.user_service.dto.UserProfileResponse;
import com.SwiftBasket.user_service.entity.User;

public interface UserService {
    User register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    User getUserProfile(Long userId);
    User updateProfile(Long userId, UserProfileResponse profileRequest);
    void initiatePasswordReset(String email);
    void resetPassword(String token, String newPassword);
    Long validateToken(String token);
}
