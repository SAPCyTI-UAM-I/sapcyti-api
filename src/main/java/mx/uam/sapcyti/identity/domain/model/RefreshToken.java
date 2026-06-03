package mx.uam.sapcyti.identity.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity within User aggregate. Persisted refresh token storing only the SHA-256 hash.
 * Supports 'Remember me' via configurable TTL and concurrent session awareness via device tracking.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash; // SHA-256

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected RefreshToken() {
        // For JPA
    }

    public RefreshToken(String tokenHash, Instant expiresAt, String deviceInfo, User user) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.deviceInfo = deviceInfo;
        this.user = user;
        this.revoked = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public User getUser() {
        return user;
    }
}
