package com.mathstrokes.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required")
        String resetToken,

        @NotBlank(message = "Please choose a new password")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).+$",
                message = "Password must contain an uppercase letter, a lowercase letter and a digit")
        String newPassword,

        @NotBlank(message = "Please confirm your new password")
        String confirmPassword) {
}
