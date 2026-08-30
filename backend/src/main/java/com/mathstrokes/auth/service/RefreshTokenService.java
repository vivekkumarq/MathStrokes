package com.mathstrokes.auth.service;

import java.time.Instant;
import java.util.UUID;

import com.mathstrokes.auth.entity.RefreshToken;
import com.mathstrokes.auth.repository.RefreshTokenRepository;
import com.mathstrokes.common.exception.ApiException;
import com.mathstrokes.common.exception.ErrorCode;
import com.mathstrokes.security.jwt.JwtService;
import com.mathstrokes.security.jwt.TokenHasher;
import com.mathstrokes.user.entity.User;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, verifies and rotates refresh tokens.
 *
 * Rotation is mandatory: presenting a refresh token revokes it and returns a new one. A second
 * use of the same token therefore fails, which both limits the value of a stolen token and gives
 * us a reuse signal.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;
    private final TokenHasher tokenHasher;

    public RefreshTokenService(RefreshTokenRepository repository, JwtService jwtService,
                               TokenHasher tokenHasher) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.tokenHasher = tokenHasher;
    }

    @Transactional
    public String issue(User user) {
        String tokenId = UUID.randomUUID().toString();
        String token = jwtService.generateRefreshToken(user.getId(), user.getPhoneNumber(), tokenId);
        persist(user, tokenId, token);
        return token;
    }

    /**
     * Verifies the presented token, revokes it, and issues a replacement.
     *
     * @return the new refresh token
     */
    @Transactional
    public RotationResult rotate(String presentedToken) {
        Claims claims = jwtService.parse(presentedToken,
                com.mathstrokes.security.jwt.TokenType.REFRESH);
        String tokenId = claims.getId();
        if (tokenId == null) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, "Refresh token is malformed");
        }

        RefreshToken stored = repository.findByTokenId(tokenId)
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID,
                        "Refresh token is no longer valid"));

        Instant now = Instant.now();
        if (!stored.isActive(now)) {
            // Presenting an already-rotated token is the signature of a stolen credential being
            // replayed. Revoke the whole family so both parties have to authenticate again.
            log.warn("Refresh token {} replayed after revocation; revoking all sessions for user {}",
                    tokenId, stored.getUser().getId());
            repository.revokeAllForUser(stored.getUser().getId(), now);
            throw new ApiException(ErrorCode.TOKEN_INVALID, "Refresh token is no longer valid");
        }
        if (!tokenHasher.matches(presentedToken, stored.getTokenHash())) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, "Refresh token is no longer valid");
        }

        User user = stored.getUser();
        String newTokenId = UUID.randomUUID().toString();
        String newToken = jwtService.generateRefreshToken(user.getId(), user.getPhoneNumber(),
                newTokenId);
        stored.revoke(now);
        stored.setReplacedBy(newTokenId);
        persist(user, newTokenId, newToken);
        return new RotationResult(user, newToken);
    }

    @Transactional
    public void revoke(String presentedToken) {
        try {
            Claims claims = jwtService.parse(presentedToken,
                    com.mathstrokes.security.jwt.TokenType.REFRESH);
            repository.findByTokenId(claims.getId())
                    .ifPresent(token -> token.revoke(Instant.now()));
        } catch (ApiException ex) {
            // Logging out with a token that is already expired or malformed is a no-op, not an
            // error: the client's intent is satisfied either way.
            log.debug("Logout presented an unusable refresh token: {}", ex.getMessage());
        }
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    private void persist(User user, String tokenId, String token) {
        RefreshToken entity = new RefreshToken();
        entity.setTokenId(tokenId);
        entity.setTokenHash(tokenHasher.hash(token));
        entity.setUser(user);
        entity.setIssuedAt(Instant.now());
        entity.setExpiresAt(jwtService.refreshTokenExpiryFromNow());
        repository.save(entity);
    }

    /** Clears out tokens that expired long enough ago to be useless even as an audit trail. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        int removed = repository.deleteExpiredBefore(
                Instant.now().minus(java.time.Duration.ofDays(30)));
        if (removed > 0) {
            log.info("Purged {} expired refresh tokens", removed);
        }
    }

    public record RotationResult(User user, String refreshToken) {
    }
}
