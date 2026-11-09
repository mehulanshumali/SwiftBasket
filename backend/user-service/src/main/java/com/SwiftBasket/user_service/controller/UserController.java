package com.SwiftBasket.user_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.SwiftBasket.user_service.dto.LoginRequest;
import com.SwiftBasket.user_service.dto.LoginResponse;
import com.SwiftBasket.user_service.dto.PasswordResetRequest;
import com.SwiftBasket.user_service.dto.RegisterRequest;
import com.SwiftBasket.user_service.dto.ResetPasswordRequest;
import com.SwiftBasket.user_service.dto.TokenValidationResponse;
import com.SwiftBasket.user_service.dto.UserProfileResponse;
import com.SwiftBasket.user_service.entity.User;
import com.SwiftBasket.user_service.mapper.UserMapper;
import com.SwiftBasket.user_service.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@Tag(name = "User Authentication", description = "User registration, login, and profile management endpoints")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with email and password. Returns 201 Created on success.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input - email or password format"),
        @ApiResponse(responseCode = "409", description = "User with this email already exists")
    })
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request received for email: {}", request.getEmail());
        User user = userService.register(request);
        UserProfileResponse response = userMapper.toProfileResponse(user);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates user with email and password. Returns JWT token on success.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input format"),
        @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves the profile information of the authenticated user. Requires Bearer token.")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User profile retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @Parameter(description = "User ID extracted from JWT token", required = true)
            @RequestHeader(value = "X-User-ID", required = true) Long userId) {
        log.info("Fetching profile for user ID: {}", userId);
        User user = userService.getUserProfile(userId);
        UserProfileResponse response = userMapper.toProfileResponse(user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @Operation(summary = "Update user profile", description = "Updates user profile information (name, phone, address). Requires Bearer token.")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Parameter(description = "User ID extracted from JWT token", required = true)
            @RequestHeader(value = "X-User-ID", required = true) Long userId,
            @Valid @RequestBody UserProfileResponse request) {
        log.info("Update profile request for user ID: {}", userId);
        User user = userService.updateProfile(userId, request);
        UserProfileResponse response = userMapper.toProfileResponse(user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete own account", description = "Soft-deletes the authenticated user's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteAccount(
            @Parameter(description = "User ID injected by Kong", required = true)
            @RequestHeader(value = "X-User-ID", required = false) Long userId) {

        if (userId == null) {
            log.warn("Account deletion attempt without X-User-ID header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset")
    @Operation(summary = "Request password reset", description = "Initiates password reset process. Sends reset link to user email.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset link sent (if email exists)"),
        @ApiResponse(responseCode = "400", description = "Invalid email format")
    })
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        log.info("Password reset request for email: {}", request.getEmail());
        userService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok("If the email exists, you will receive a password reset link");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token", description = "Completes password reset using the token sent to user email.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or expired token"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt");
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Password reset successfully");
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token", description = "Validates JWT token and returns userId, email and role. Used by Kong for session validation.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid, returns user ID"),
        @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    public ResponseEntity<TokenValidationResponse> validateToken(
            @Parameter(description = "JWT token in format: Bearer <token>", required = true)
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Token validation request received");

        // Check if Authorization header is present
        if (authHeader == null || authHeader.isEmpty()) {
            log.warn("Authorization header is missing");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract token from "Bearer <token>"
        String token;
        if (authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Remove "Bearer " prefix
        } else {
            log.warn("Invalid Authorization header format. Expected 'Bearer <token>'");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (token.isEmpty()) {
            log.warn("Token is empty");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TokenValidationResponse response = userService.validateToken(token);

        if (response != null) {
            log.info("Token validated successfully for user ID: {}", response.getUserId());
            return ResponseEntity.ok(response);
        }

        log.warn("Invalid or expired token");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint to verify service is running.")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is running");
    }
}
