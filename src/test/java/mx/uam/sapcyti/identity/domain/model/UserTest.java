package mx.uam.sapcyti.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("should create user with correct initial state")
    void shouldCreateUserWithCorrectInitialState() {
        User user = new User("test@uam.mx", "hash", RoleType.STUDENT, 1L);

        assertThat(user.getEmail()).isEqualTo("test@uam.mx");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getRole()).isEqualTo(RoleType.STUDENT);
        assertThat(user.getGraduateProgramId()).isEqualTo(1L);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getRefreshTokens()).isEmpty();
    }
}
