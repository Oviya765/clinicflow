package com.HospitalManagement.requestdto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank String newPassword
) {}
