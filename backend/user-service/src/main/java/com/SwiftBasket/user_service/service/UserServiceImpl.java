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
import com.SwiftBasket.user_service.entity.User;
import com.SwiftBasket.user_service.event.UserRegisteredEvent;
import com.SwiftBasket.user_service.exception.InvalidCredentialsException;
import com.SwiftBasket.user_service.exception.UserAlreadyExistsException;
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
        return null;
    }

    @Override
    public User updateProfile(Long userId, UserProfileResponse profileRequest) {
        return null;
    }

    @Override
    public void initiatePasswordReset(String email) {
        return;
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        return;
    }
    @Override
    public Long validateToken(String token) {
        return null;
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
