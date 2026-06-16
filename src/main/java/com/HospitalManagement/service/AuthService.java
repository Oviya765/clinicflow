package com.HospitalManagement.service;

import com.HospitalManagement.entity.Patient;
import com.HospitalManagement.entity.User;
import com.HospitalManagement.enums.Roles;
import com.HospitalManagement.repository.PatientRepository;
import com.HospitalManagement.repository.UserRepository;
import com.HospitalManagement.requestdto.AuthenticationRequest;
import com.HospitalManagement.requestdto.RegisterRequest;
import com.HospitalManagement.responsedto.AuthenticationResponse;
import com.HospitalManagement.responsedto.UserResponseDto;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        logger.info("Attempting to register new user with email: {}", request.getEmail());
        
        if (repository.existsByEmail(request.getEmail())) {
            logger.warn("Registration failed: User already exists with email: {}", request.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User already exists with email: "
            );
        }

        Roles role = Roles.PATIENT;
        var user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .status("ACTIVE")
                .build();
        repository.save(user);
        
        logger.info("User registered successfully - Name: {}, Email: {}, Phone: {}, Role: {}", 
            request.getName(), request.getEmail(), request.getPhone(), role);

        return AuthenticationResponse.builder()
                .message("User registered successfully")
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        logger.info("Attempting authentication for user with email: {}", request.getEmail());
        
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            logger.warn("Authentication failed: Invalid credentials for email: {}", request.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Authentication failed: User not found for email: {}", request.getEmail());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        boolean needsPasswordChange = passwordEncoder.matches("clinic@123", user.getPassword());
        var jwtToken = jwtService.generateToken(user, needsPasswordChange);
        
        logger.info("User authenticated successfully - Email: {}, Role: {}", request.getEmail(), user.getRole());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("User authenticated successfully")
                .build();
    }

    public User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        return repository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authenticated user not found"
                        ));
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Password must be at least 8 characters long"
            );
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Password must contain at least one uppercase letter"
            );
        }
        if (!password.matches(".*[a-z].*")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Password must contain at least one lowercase letter"
            );
        }
        if (!password.matches(".*[0-9].*")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Password must contain at least one numeric digit"
            );
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Password must contain at least one special character"
            );
        }
        if ("clinic@123".equals(password)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Password cannot be the default clinic@123 password"
            );
        }
    }

    public AuthenticationResponse changePassword(String email, String newPassword) {
        validatePasswordStrength(newPassword);
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
        
        String newToken = jwtService.generateToken(user, false);
        return AuthenticationResponse.builder()
                .token(newToken)
                .message("Password changed successfully")
                .build();
    }
     public UserResponseDto getCurrentUserProfile() {
        User user = getAuthenticatedUser();
        return new UserResponseDto(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}