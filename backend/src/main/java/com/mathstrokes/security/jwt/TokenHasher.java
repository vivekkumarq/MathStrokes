package com.mathstrokes.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Hashes refresh tokens for storage.
 *
 * SHA-256 rather than BCrypt, for two reasons. A refresh token is a signed JWT carrying a random
 * UUID, so it already has far more entropy than any password - the work factor BCrypt exists to
 * add buys nothing against a brute-force search of that space. And BCrypt refuses inputs over 72
 * bytes, which a JWT comfortably exceeds.
 *
 * What matters here is that a database leak yields no usable token, and that holds: the stored
 * digest cannot be presented to the API.
 */
@Component
public class TokenHasher {

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required but unavailable", ex);
        }
    }

    /** Constant-time comparison, so timing cannot be used to narrow down a stored digest. */
    public boolean matches(String token, String storedHash) {
        if (token == null || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(token).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}
