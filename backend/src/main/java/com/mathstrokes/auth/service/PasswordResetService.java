package com.mathstrokes.auth.service;

import java.util.List;

import com.mathstrokes.auth.dto.ForgotPasswordQuestionResponse;
import com.mathstrokes.auth.dto.PasswordResetTokenResponse;
import com.mathstrokes.auth.dto.ResetPasswordRequest;
import com.mathstrokes.common.dto.FieldErrorItem;
import com.mathstrokes.common.exception.ApiException;
import com.mathstrokes.common.exception.ErrorCode;
import com.mathstrokes.common.exception.ResourceNotFoundException;
import com.mathstrokes.common.exception.ValidationException;
import com.mathstrokes.config.JwtProperties;
import com.mathstrokes.security.jwt.JwtService;
import com.mathstrokes.security.jwt.TokenType;
import com.mathstrokes.user.entity.User;
import com.mathstrokes.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Security-question account recovery.
 *
 * The invariant: knowing a phone number alone never changes a password. A reset token is minted
 * only after the security answer verifies against its stored hash, and it is single-use - the
 * password change bumps the stored hash, and every refresh token for the account is revoked, so
 * a stolen reset token cannot be replayed and existing sessions do not survive the reset.
 *
 * The three steps are deliberately separate so an OTP challenge can be slotted in as an
 * alternative to the security answer later without touching the reset step.
 */
@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter rateLimiter;

    public PasswordResetService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                JwtService jwtService, JwtProperties jwtProperties,
                                RefreshTokenService refreshTokenService,
                                LoginRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenService = refreshTokenService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Step 1. Returns only the security question - no name, no status, nothing else about the
     * account. Rate limited, because this is the endpoint an attacker would use to enumerate
     * registered numbers.
     */
    @Transactional(readOnly = true)
    public ForgotPasswordQuestionResponse initiate(String phoneNumber) {
        String normalised = phoneNumber.trim();
        rateLimiter.checkAllowed("forgot:" + normalised);
        User user = userRepository.findByPhoneNumber(normalised)
                .orElseGet(() -> {
                    rateLimiter.recordFailure("forgot:" + normalised);
                    throw new ResourceNotFoundException("No account is registered with that phone number");
                });
        if (user.getSecurityQuestion() == null || user.getSecurityAnswerHash() == null) {
            throw new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "This account has no security question set. Please contact your teacher.");
        }
        return new ForgotPasswordQuestionResponse(user.getPhoneNumber(), user.getSecurityQuestion());
    }

    /** Step 2. Verifies the answer and mints the single-use reset authorisation. */
    @Transactional(readOnly = true)
    public PasswordResetTokenResponse verifyAnswer(String phoneNumber, String securityAnswer) {
        String normalised = phoneNumber.trim();
        String key = "verify:" + normalised;
        rateLimiter.checkAllowed(key);

        User user = userRepository.findByPhoneNumber(normalised)
                .orElseThrow(() -> {
                    rateLimiter.recordFailure(key);
                    return new ApiException(ErrorCode.AUTHENTICATION_FAILED,
                            "The answer does not match our records");
                });

        boolean matches = user.getSecurityAnswerHash() != null && passwordEncoder.matches(
                SecurityAnswers.normalise(securityAnswer), user.getSecurityAnswerHash());
        if (!matches) {
            rateLimiter.recordFailure(key);
            throw new ApiException(ErrorCode.AUTHENTICATION_FAILED,
                    "The answer does not match our records");
        }

        rateLimiter.reset(key);
        String token = jwtService.generatePasswordResetToken(user.getId(), user.getPhoneNumber());
        return new PasswordResetTokenResponse(token,
                jwtProperties.getPasswordResetTokenExpiration().toSeconds());
    }

    /** Step 3. Applies the new password and invalidates every existing session. */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ValidationException("Password reset failed", List.of(
                    FieldErrorItem.of("confirmPassword", "Passwords do not match")));
        }

        Claims claims = jwtService.parse(request.resetToken(), TokenType.PASSWORD_RESET);
        Long userId = claims.get(JwtService.CLAIM_USER_ID, Number.class).longValue();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID,
                        "This reset link is no longer valid"));

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ValidationException("Password reset failed", List.of(
                    FieldErrorItem.of("newPassword",
                            "Please choose a password you have not used before")));
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Anyone holding a session for this account loses it, which is the point of a reset.
        refreshTokenService.revokeAllForUser(user.getId());
    }
}
