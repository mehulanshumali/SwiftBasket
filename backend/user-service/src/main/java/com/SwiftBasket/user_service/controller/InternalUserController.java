package com.SwiftBasket.user_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.SwiftBasket.user_service.dto.UserEmailResponse;
import com.SwiftBasket.user_service.entity.User;
import com.SwiftBasket.user_service.mapper.UserMapper;
import com.SwiftBasket.user_service.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal endpoints called service-to-service (never via Kong).
 * Endpoint 1.8: claim-check email resolution for notification-service.
 */
@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@Tag(name = "Internal Users", description = "Internal endpoints for service-to-service calls (claim-check email resolution)")
public class InternalUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/{id}/email")
    @Operation(summary = "Get user email by ID", description = "Internal endpoint used by notification-service "
            + "to resolve a recipient's current email at send time (claim-check pattern - events carry userId, not PII).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User email returned"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserEmailResponse> getUserEmail(
            @Parameter(description = "User ID", required = true) @PathVariable("id") Long id) {

        log.info("Internal email lookup for userId: {}", id);
        User user = userService.getUserProfile(id);
        return ResponseEntity.ok(userMapper.toEmailResponse(user));
    }
}
