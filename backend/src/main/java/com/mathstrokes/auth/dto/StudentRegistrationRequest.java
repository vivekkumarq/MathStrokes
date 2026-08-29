package com.mathstrokes.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Field names here are the exact keys the Angular registration form binds to, and validation
 * messages are written to be shown to a student verbatim.
 */
public record StudentRegistrationRequest(
        @NotBlank(message = "Please enter your full name")
        @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
        String fullName,

        @NotBlank(message = "Please enter your phone number")
        @Pattern(regexp = "^[0-9]{10,15}$",
                message = "Phone number must be 10 to 15 digits, with no spaces or symbols")
        String phoneNumber,

        @NotBlank(message = "Please choose a password")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).+$",
                message = "Password must contain an uppercase letter, a lowercase letter and a digit")
        String password,

        @NotBlank(message = "Please confirm your password")
        String confirmPassword,

        @NotBlank(message = "Please choose a security question")
        @Size(max = 255, message = "Security question must be at most 255 characters")
        String securityQuestion,

        @NotBlank(message = "Please answer your security question")
        @Size(min = 2, max = 200, message = "Security answer must be between 2 and 200 characters")
        String securityAnswer) {
}
