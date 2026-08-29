package com.mathstrokes.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Please enter your phone number")
        String phoneNumber,

        @NotBlank(message = "Please enter your password")
        String password) {
}
