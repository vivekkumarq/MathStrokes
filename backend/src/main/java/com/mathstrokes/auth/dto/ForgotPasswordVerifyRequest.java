package com.mathstrokes.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordVerifyRequest(
        @NotBlank(message = "Please enter your phone number")
        String phoneNumber,

        @NotBlank(message = "Please answer your security question")
        String securityAnswer) {
}
