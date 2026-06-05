package mx.uam.sapcyti.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

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

    public static PasswordResetToken create(String tokenHash, int ttlMinutes) {
        return new PasswordResetToken(tokenHash, Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES));
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return used;
    }

    public boolean isValidForReset() {
        return !isUsed() && !isExpired();
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
