package com.HospitalManagement.controller;

import com.HospitalManagement.requestdto.AuthenticationRequest;
import com.HospitalManagement.requestdto.ChangePasswordRequest;
import com.HospitalManagement.requestdto.RegisterRequest;
import com.HospitalManagement.responsedto.AuthenticationResponse;
import com.HospitalManagement.responsedto.UserResponseDto;
import com.HospitalManagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Patient self‑signup
     * Flow: User(PATIENT) → Patient → JWT
     */
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request
    ) {
        AuthenticationResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        AuthenticationResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<AuthenticationResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AuthenticationResponse response = authService.changePassword(email, request.newPassword());
        return ResponseEntity.ok(response);
    }

     @GetMapping("/me")
    public ResponseEntity<UserResponseDto> me() {
        UserResponseDto response = authService.getCurrentUserProfile();
        return ResponseEntity.ok(response);
    }
}