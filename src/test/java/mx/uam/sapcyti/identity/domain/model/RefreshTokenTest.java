package mx.uam.sapcyti.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    @Test
    @DisplayName("should correctly identify expired token")
    void shouldCorrectlyIdentifyExpiredToken() {
        User user = new User("test@uam.mx", "hash", RoleType.STUDENT, 1L);
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        RefreshToken token = new RefreshToken("tokenHash", past, "device", user);

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    @DisplayName("should correctly identify non-expired token")
    void shouldCorrectlyIdentifyNonExpiredToken() {
        User user = new User("test@uam.mx", "hash", RoleType.STUDENT, 1L);
        Instant future = Instant.now().plus(1, ChronoUnit.HOURS);
        RefreshToken token = new RefreshToken("tokenHash", future, "device", user);

        assertThat(token.isExpired()).isFalse();
    }
}
