package com.mathstrokes.auth.controller;

import java.util.List;

import com.mathstrokes.auth.dto.AuthResponse;
import com.mathstrokes.auth.dto.ForgotPasswordInitiateRequest;
import com.mathstrokes.auth.dto.ForgotPasswordQuestionResponse;
import com.mathstrokes.auth.dto.ForgotPasswordVerifyRequest;
import com.mathstrokes.auth.dto.LoginRequest;
import com.mathstrokes.auth.dto.PasswordResetTokenResponse;
import com.mathstrokes.auth.dto.RefreshRequest;
import com.mathstrokes.auth.dto.ResetPasswordRequest;
import com.mathstrokes.auth.dto.SecurityQuestionResponse;
import com.mathstrokes.auth.dto.StudentRegistrationRequest;
import com.mathstrokes.auth.service.AuthService;
import com.mathstrokes.auth.service.PasswordResetService;
import com.mathstrokes.auth.service.SecurityQuestionCatalog;
import com.mathstrokes.common.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public authentication surface. Every route here is reachable without a token. */
@RestController
@RequestMapping("/auth")
@SecurityRequirements
@Tag(name = "Authentication", description = "Registration, login, token refresh and recovery")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final SecurityQuestionCatalog securityQuestionCatalog;

    public AuthController(AuthService authService, PasswordResetService passwordResetService,
                          SecurityQuestionCatalog securityQuestionCatalog) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.securityQuestionCatalog = securityQuestionCatalog;
    }

    @GetMapping("/security-questions")
    @Operation(summary = "The security questions offered at registration",
            description = "Submit the question text, not the id, when registering.")
    public List<SecurityQuestionResponse> securityQuestions() {
        return securityQuestionCatalog.all();
    }

    @PostMapping("/student/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a student account",
            description = "Always creates a ROLE_STUDENT account; the role cannot be chosen. "
                    + "Returns a signed-in session so the student does not have to log in again.")
    public AuthResponse register(@Valid @RequestBody StudentRegistrationRequest request) {
        return authService.registerStudent(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with a phone number and password")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new session",
            description = "Rotates both tokens. The presented refresh token is revoked, so the "
                    + "client must replace both values from the response.")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token",
            description = "Idempotent: an expired or unknown token still returns success.")
    public MessageResponse logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return new MessageResponse("Signed out");
    }

    @PostMapping("/forgot-password/initiate")
    @Operation(summary = "Recovery step 1: fetch the account's security question")
    public ForgotPasswordQuestionResponse forgotPasswordInitiate(
            @Valid @RequestBody ForgotPasswordInitiateRequest request) {
        return passwordResetService.initiate(request.phoneNumber());
    }

    @PostMapping("/forgot-password/verify")
    @Operation(summary = "Recovery step 2: answer the security question",
            description = "Returns a short-lived, single-use token authorising one password change.")
    public PasswordResetTokenResponse forgotPasswordVerify(
            @Valid @RequestBody ForgotPasswordVerifyRequest request) {
        return passwordResetService.verifyAnswer(request.phoneNumber(), request.securityAnswer());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Recovery step 3: set the new password",
            description = "Signs out every existing session for the account.")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return new MessageResponse("Your password has been updated. Please sign in.");
    }
}
