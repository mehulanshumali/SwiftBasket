package com.SwiftBasket.user_service.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.SwiftBasket.user_service.dto.LoginRequest;
import com.SwiftBasket.user_service.dto.LoginResponse;
import com.SwiftBasket.user_service.dto.RegisterRequest;
import com.SwiftBasket.user_service.dto.UserProfileResponse;
import com.SwiftBasket.user_service.entity.PasswordResetToken;
import com.SwiftBasket.user_service.entity.User;
import com.SwiftBasket.user_service.event.UserRegisteredEvent;
import com.SwiftBasket.user_service.exception.InvalidCredentialsException;
import com.SwiftBasket.user_service.exception.UserAlreadyExistsException;
import com.SwiftBasket.user_service.exception.UserNotFoundException;
import com.SwiftBasket.user_service.repository.PasswordResetTokenRepository;
import com.SwiftBasket.user_service.repository.UserRepository;
import com.SwiftBasket.user_service.util.JwtTokenProvider;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private static final long PASSWORD_RESET_TOKEN_EXPIRATION_HOURS = 1;

    @Value("${kafka_topic.name}")
    private String userRegisteredTopic;

    @Override
    public User register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("User already exists with email: {}", request.getEmail());
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .fullName(request.getFullName())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        publishUserRegisteredEvent(savedUser);

        return savedUser;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("User login attempt with email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", request.getEmail());
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for email: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password"); 
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        long expirationTimeInMs = jwtTokenProvider.getExpirationTimeInMs();

        log.info("User logged in successfully: {}", user.getEmail());
        
        return LoginResponse.builder()
                .token(token)
                .expiresIn(expirationTimeInMs)
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Override
    public User getUserProfile(Long userId) {
        log.info("Fetching user profile for user ID: {}", userId);
        return userRepository.findById(userId).orElseThrow(() -> {
            log.warn("User not found with ID: {}", userId);
            return new UserNotFoundException("User not found");
        });
    }

    @Override
    public User updateProfile(Long userId, UserProfileResponse profileRequest) {
        log.info("Updating user profile for user ID: {}", userId);
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.warn("User not found with ID: {}", userId);
            return new UserNotFoundException("User not found");
        });

        if(profileRequest.getFullName() != null) {
            user.setFullName(profileRequest.getFullName());
        }

        if(profileRequest.getPhoneNumber() != null) {
            user.setPhoneNumber(profileRequest.getPhoneNumber());
        }

        if(profileRequest.getAddress() != null) {
            user.setAddress(profileRequest.getAddress());
        }

        if(profileRequest.getEmail() != null && !profileRequest.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(profileRequest.getEmail())) {
                log.warn("Email already in use: {}", profileRequest.getEmail());
                throw new UserAlreadyExistsException("Email is already in use by another account");
            }
            user.setEmail(profileRequest.getEmail());
        }

        User updatedUser = userRepository.save(user);
        log.info("User profile updated successfully for user ID: {}", userId);
        return updatedUser;
        
    }

    @Override
    public void initiatePasswordReset(String email) {
        log.info("Password reset initiated for email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        String resetToken = PasswordResetToken.generateToken();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(PASSWORD_RESET_TOKEN_EXPIRATION_HOURS);
        passwordResetTokenRepository.deleteByUserIdAndIsUsedTrue(user.getId());
        PasswordResetToken token = PasswordResetToken.builder()
                .token(resetToken)
                .user(user)
                .expiryDate(expiryDate)
                .isUsed(false)
                .build();
        passwordResetTokenRepository.save(token);
        log.info("Password reset token generated for user ID: {}", user.getId());

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetToken);
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
        }
}

    @Override
    public void resetPassword(String token, String newPassword) {
        log.info("Password reset attempt with token");
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Invalid password reset token");
                    return new InvalidCredentialsException("Invalid or expired password reset token");
                });
        if (!resetToken.isValid()) {
            log.warn("Expired or already used password reset token");
            throw new InvalidCredentialsException("Invalid or expired password reset token");
        }
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset successfully for user ID: {}", user.getId());

        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);
        log.info("Password reset completed successfully for user ID: {}", user.getId());    
    }

    @Override
    public Long validateToken(String token) {
        log.info("Validating token");

        if (token == null || token.isEmpty()) {
            log.warn("Token is null or empty");
            throw new InvalidCredentialsException("Invalid or expired token");
        }
        
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Token validation failed");
            throw new InvalidCredentialsException("Invalid or expired token");
        }
        
        if (jwtTokenProvider.isTokenExpired(token)) {
            log.warn("Token has expired");
            throw new InvalidCredentialsException("Invalid or expired token");
        }
        
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        if (userId == null) {
            log.warn("Could not extract user ID from token");
            throw new InvalidCredentialsException("Invalid or expired token");
        }
        
        log.info("Token validated successfully for user ID: {}", userId);
        return userId;
    }

    private void publishUserRegisteredEvent(User user) {
        try {
            UserRegisteredEvent event = UserRegisteredEvent.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .registeredAt(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(userRegisteredTopic, event.getUserId().toString(), event);
            log.info("User registered event published to Kafka for user ID: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish user registered event: {}", e.getMessage(), e);        }
    }
}
