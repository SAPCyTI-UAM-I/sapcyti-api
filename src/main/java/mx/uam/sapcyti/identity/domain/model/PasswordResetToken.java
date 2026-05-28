package mx.uam.sapcyti.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * Value Object. Time-limited token for password recovery flow (HU-02).
 * SHA-256 hashed, 30 min TTL, single-use.
 */
@Embeddable
public class PasswordResetToken {

    @Column(name = "password_reset_token_hash")
    private String tokenHash;

    @Column(name = "password_reset_token_expires_at")
    private Instant expiresAt;

    @Column(name = "password_reset_token_used")
    private boolean used = false;

    protected PasswordResetToken() {
        // For JPA
    }

    public PasswordResetToken(String tokenHash, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.used = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return used;
    }

    public void markAsUsed() {
        this.used = true;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
