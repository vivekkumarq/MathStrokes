package com.mathstrokes.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.mathstrokes.common.exception.ApiException;
import com.mathstrokes.common.exception.ErrorCode;
import com.mathstrokes.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies the three token flavours used by the platform.
 *
 * Every token carries a {@code typ} claim so an access token can never be replayed as a refresh
 * token or as a password-reset authorisation, and vice versa.
 */
@Service
public class JwtService {

    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_USER_ID = "uid";
    /** Fingerprint of the password the reset token was minted against. */
    public static final String CLAIM_PASSWORD_FINGERPRINT = "pwf";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "mathstrokes.jwt.secret must be at least 32 characters (256 bits). "
                            + "Set the JWT_SECRET environment variable to a strong random value.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String phoneNumber, List<String> roles) {
        return buildToken(TokenType.ACCESS, userId, phoneNumber, roles,
                properties.getAccessTokenExpiration(), null);
    }

    /**
     * @param tokenId opaque identifier persisted alongside the refresh token row, so a refresh
     *                token can be revoked server-side on logout or rotation.
     */
    public String generateRefreshToken(Long userId, String phoneNumber, String tokenId) {
        return buildToken(TokenType.REFRESH, userId, phoneNumber, List.of(),
                properties.getRefreshTokenExpiration(), tokenId);
    }

    /**
     * @param passwordFingerprint digest of the password hash in force right now. Checking it at
     *                            redemption is what makes the token single-use: applying a new
     *                            password changes the hash, so the fingerprint stops matching and
     *                            the token is dead even though it has not expired.
     */
    public String generatePasswordResetToken(Long userId, String phoneNumber,
                                             String passwordFingerprint) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(phoneNumber)
                .claim(CLAIM_TOKEN_TYPE, TokenType.PASSWORD_RESET.name())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_PASSWORD_FINGERPRINT, passwordFingerprint)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getPasswordResetTokenExpiration())))
                .signWith(signingKey)
                .compact();
    }

    private String buildToken(TokenType type, Long userId, String subject, List<String> roles,
                              Duration ttl, String jti) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(subject)
                .claim(CLAIM_TOKEN_TYPE, type.name())
                .claim(CLAIM_USER_ID, userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)));
        if (!roles.isEmpty()) {
            builder.claim(CLAIM_ROLES, roles);
        }
        if (jti != null) {
            builder.id(jti);
        }
        return builder.signWith(signingKey).compact();
    }

    /**
     * Verifies signature, expiry, issuer and token type.
     *
     * @throws ApiException with TOKEN_EXPIRED or TOKEN_INVALID; never leaks parser internals.
     */
    public Claims parse(String token, TokenType expectedType) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jws.getPayload();
            String actualType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!expectedType.name().equals(actualType)) {
                throw new ApiException(ErrorCode.TOKEN_INVALID, "Token is not a "
                        + expectedType.name().toLowerCase() + " token");
            }
            return claims;
        } catch (ExpiredJwtException ex) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED, "Token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, "Token is invalid");
        }
    }

    public Instant refreshTokenExpiryFromNow() {
        return Instant.now().plus(properties.getRefreshTokenExpiration());
    }

    public long accessTokenExpiresInSeconds() {
        return properties.getAccessTokenExpiration().toSeconds();
    }
}
