package com.mathstrokes.auth.service;

import java.time.Duration;
import java.util.Optional;

import com.mathstrokes.auth.dto.PasswordResetTokenResponse;
import com.mathstrokes.auth.dto.ResetPasswordRequest;
import com.mathstrokes.common.exception.ApiException;
import com.mathstrokes.common.exception.ErrorCode;
import com.mathstrokes.config.AppProperties;
import com.mathstrokes.config.JwtProperties;
import com.mathstrokes.security.jwt.JwtService;
import com.mathstrokes.security.jwt.TokenHasher;
import com.mathstrokes.user.entity.User;
import com.mathstrokes.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Recovery is the softest part of any account system, so these assert the two properties that
 * matter: knowing a phone number alone gets you nothing, and a reset token works exactly once.
 *
 * The single-use property was originally documented but not implemented - a captured token could
 * be replayed for its full lifetime. This test exists so that cannot regress silently.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenService refreshTokenService;

    private PasswordResetService service;
    private PasswordEncoder passwordEncoder;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("a-test-signing-secret-that-is-long-enough-32");
        jwtProperties.setPasswordResetTokenExpiration(Duration.ofMinutes(10));

        AppProperties appProperties = new AppProperties();
        appProperties.getRateLimit().setEnabled(false);

        // Strength 4 keeps the test fast; production uses 12.
        passwordEncoder = new BCryptPasswordEncoder(4);
        JwtService jwtService = new JwtService(jwtProperties);
        TokenHasher tokenHasher = new TokenHasher();

        service = new PasswordResetService(userRepository, passwordEncoder, jwtService,
                jwtProperties, refreshTokenService, new LoginRateLimiter(appProperties), tokenHasher);

        user = new User();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setFullName("Ananya Sharma");
        user.setPhoneNumber("9812345670");
        user.setPasswordHash(passwordEncoder.encode("OldPass@1"));
        user.setSecurityQuestion("In which city were you born?");
        user.setSecurityAnswerHash(passwordEncoder.encode(SecurityAnswers.normalise("Delhi")));

        when(userRepository.findByPhoneNumber("9812345670")).thenReturn(Optional.of(user));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    }

    private String mintToken(String answer) {
        PasswordResetTokenResponse response = service.verifyAnswer("9812345670", answer);
        return response.resetToken();
    }

    @Test
    @DisplayName("the security question is returned, and nothing else about the account")
    void initiateReturnsOnlyTheQuestion() {
        var response = service.initiate("9812345670");

        assertThat(response.securityQuestion()).isEqualTo("In which city were you born?");
        assertThat(response.phoneNumber()).isEqualTo("9812345670");
    }

    @Test
    @DisplayName("a wrong security answer mints no token")
    void wrongAnswerIsRejected() {
        assertThatThrownBy(() -> service.verifyAnswer("9812345670", "Mumbai"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTHENTICATION_FAILED));
    }

    @Test
    @DisplayName("the answer matches regardless of casing and stray spacing")
    void answerMatchingIsForgiving() {
        assertThat(mintToken("  DELHI ")).isNotBlank();
    }

    @Test
    @DisplayName("a valid token changes the password and revokes every session")
    void resetAppliesTheNewPassword() {
        String token = mintToken("Delhi");

        service.resetPassword(new ResetPasswordRequest(token, "NewPass@1", "NewPass@1"));

        assertThat(passwordEncoder.matches("NewPass@1", user.getPasswordHash())).isTrue();
        org.mockito.Mockito.verify(refreshTokenService).revokeAllForUser(7L);
    }

    @Test
    @DisplayName("a reset token cannot be used twice, even inside its lifetime")
    void resetTokenIsSingleUse() {
        String token = mintToken("Delhi");
        service.resetPassword(new ResetPasswordRequest(token, "NewPass@1", "NewPass@1"));

        // Same token, still well within its ten minutes.
        assertThatThrownBy(() ->
                service.resetPassword(new ResetPasswordRequest(token, "Attacker@9", "Attacker@9")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already been used");

        // And the attacker's password was not applied.
        assertThat(passwordEncoder.matches("Attacker@9", user.getPasswordHash())).isFalse();
        assertThat(passwordEncoder.matches("NewPass@1", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("mismatched confirmation is refused before the token is even examined")
    void confirmationMustMatch() {
        String token = mintToken("Delhi");

        assertThatThrownBy(() ->
                service.resetPassword(new ResetPasswordRequest(token, "NewPass@1", "Different@2")))
                .isInstanceOf(com.mathstrokes.common.exception.ValidationException.class);
    }

    @Test
    @DisplayName("reusing the current password is refused")
    void newPasswordMustDiffer() {
        String token = mintToken("Delhi");

        assertThatThrownBy(() ->
                service.resetPassword(new ResetPasswordRequest(token, "OldPass@1", "OldPass@1")))
                .isInstanceOf(com.mathstrokes.common.exception.ValidationException.class);
    }
}
