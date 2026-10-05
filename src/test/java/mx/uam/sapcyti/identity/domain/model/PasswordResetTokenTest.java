package mx.uam.sapcyti.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordResetTokenTest {

    @Test
    @DisplayName("create sets expiry based on TTL minutes")
    void createSetsExpiry() {
        PasswordResetToken token = PasswordResetToken.create("abc123", 30);

        assertThat(token.getTokenHash()).isEqualTo("abc123");
        assertThat(token.isUsed()).isFalse();
        assertThat(token.getExpiresAt()).isAfter(Instant.now().plus(29, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("hashToken produces deterministic SHA-256 hex")
    void hashTokenIsDeterministic() {
        String hash1 = PasswordResetToken.hashToken("test-token");
        String hash2 = PasswordResetToken.hashToken("test-token");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
    }

    @Test
    @DisplayName("isValidForReset is false when used or expired")
    void validityRules() {
        PasswordResetToken fresh = PasswordResetToken.create("hash", 30);
        assertThat(fresh.isValidForReset()).isTrue();

        fresh.markAsUsed();
        assertThat(fresh.isValidForReset()).isFalse();

        PasswordResetToken expired = new PasswordResetToken("hash", Instant.now().minus(1, ChronoUnit.MINUTES));
        assertThat(expired.isValidForReset()).isFalse();
    }
}
