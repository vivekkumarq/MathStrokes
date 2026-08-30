package com.mathstrokes.auth.service;

import java.time.Instant;
import java.util.List;

import com.mathstrokes.auth.dto.AuthResponse;
import com.mathstrokes.auth.dto.LoginRequest;
import com.mathstrokes.auth.dto.StudentRegistrationRequest;
import com.mathstrokes.auth.dto.UserProfileResponse;
import com.mathstrokes.common.dto.FieldErrorItem;
import com.mathstrokes.common.enums.RoleName;
import com.mathstrokes.common.exception.ApiException;
import com.mathstrokes.common.exception.ErrorCode;
import com.mathstrokes.common.exception.ValidationException;
import com.mathstrokes.security.jwt.JwtService;
import com.mathstrokes.user.entity.Role;
import com.mathstrokes.user.entity.User;
import com.mathstrokes.user.repository.RoleRepository;
import com.mathstrokes.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter rateLimiter;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       JwtService jwtService, RefreshTokenService refreshTokenService,
                       LoginRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Self-service student registration. Admin accounts are never created through this path;
     * the role is fixed to ROLE_STUDENT here rather than taken from the request.
     */
    @Transactional
    public AuthResponse registerStudent(StudentRegistrationRequest request) {
        String phoneNumber = request.phoneNumber().trim();

        if (!request.password().equals(request.confirmPassword())) {
            throw new ValidationException("Registration failed", List.of(
                    FieldErrorItem.of("confirmPassword", "Passwords do not match")));
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new ValidationException("Registration failed", List.of(
                    FieldErrorItem.of("phoneNumber",
                            "An account already exists for this phone number")));
        }

        Role studentRole = roleRepository.findByName(RoleName.ROLE_STUDENT)
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_STUDENT is missing. Has the reference-data migration run?"));

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setSecurityQuestion(request.securityQuestion().trim());
        // Normalised before hashing so casing and stray spaces at reset time do not matter.
        user.setSecurityAnswerHash(passwordEncoder.encode(SecurityAnswers.normalise(request.securityAnswer())));
        user.setEnabled(true);
        user.addRole(studentRole);

        User saved = userRepository.save(user);
        return issueTokens(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String phoneNumber = request.phoneNumber().trim();
        rateLimiter.checkAllowed(phoneNumber);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(phoneNumber, request.password()));
        } catch (AuthenticationException ex) {
            rateLimiter.recordFailure(phoneNumber);
            // Deliberately identical whether the account is missing or the password is wrong,
            // so the endpoint cannot be used to discover which numbers are registered.
            throw new ApiException(ErrorCode.AUTHENTICATION_FAILED,
                    "Incorrect phone number or password");
        }

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_FAILED,
                        "Incorrect phone number or password"));
        rateLimiter.reset(phoneNumber);
        user.setLastLoginAt(Instant.now());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String presentedRefreshToken) {
        RefreshTokenService.RotationResult rotation =
                refreshTokenService.rotate(presentedRefreshToken);
        User user = rotation.user();
        if (!user.isEnabled()) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "This account has been disabled");
        }
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getPhoneNumber(),
                roleNames(user));
        return AuthResponse.of(accessToken, rotation.refreshToken(),
                jwtService.accessTokenExpiresInSeconds(), toProfile(user));
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getPhoneNumber(),
                roleNames(user));
        String refreshToken = refreshTokenService.issue(user);
        return AuthResponse.of(accessToken, refreshToken,
                jwtService.accessTokenExpiresInSeconds(), toProfile(user));
    }

    private List<String> roleNames(User user) {
        return user.getRoles().stream().map(role -> role.getName().name()).sorted().toList();
    }

    public UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(user.getId(), user.getFullName(), user.getPhoneNumber(),
                roleNames(user), user.isEnabled(), user.getCreatedAt());
    }
}
