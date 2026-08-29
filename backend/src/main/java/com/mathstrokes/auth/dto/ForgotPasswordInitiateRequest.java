package com.mathstrokes.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordInitiateRequest(
        @NotBlank(message = "Please enter your phone number")
        String phoneNumber) {
}
