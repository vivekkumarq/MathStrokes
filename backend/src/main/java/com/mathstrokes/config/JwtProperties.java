package com.mathstrokes.config;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "mathstrokes.jwt")
public class JwtProperties {

    /**
     * HMAC signing secret. Supplied through the JWT_SECRET environment variable.
     * Must decode to at least 256 bits; the application refuses to start otherwise.
     */
    @NotBlank
    private String secret;

    @NotNull
    private Duration accessTokenExpiration = Duration.ofMinutes(15);

    @NotNull
    private Duration refreshTokenExpiration = Duration.ofDays(7);

    /** Lifetime of the single-use token issued after a successful security-answer challenge. */
    @NotNull
    private Duration passwordResetTokenExpiration = Duration.ofMinutes(10);

    private String issuer = "mathstrokes";
}
