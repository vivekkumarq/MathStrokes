package com.mathstrokes.auth.entity;

import java.time.Instant;

import com.mathstrokes.common.domain.BaseEntity;
import com.mathstrokes.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Server-side record of an issued refresh token, which is what makes logout and rotation real
 * rather than advisory.
 *
 * Only a BCrypt hash of the token is stored, so a database leak yields nothing replayable.
 * {@code tokenId} is the JWT id claim and is the lookup key.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken extends BaseEntity {

    @Column(name = "token_id", nullable = false, unique = true, length = 64)
    private String tokenId;

    @Column(name = "token_hash", nullable = false, length = 100)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Set when this token was rotated, pointing at its successor. Useful for reuse detection. */
    @Column(name = "replaced_by", length = 64)
    private String replacedBy;

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            this.revokedAt = now;
        }
    }
}
