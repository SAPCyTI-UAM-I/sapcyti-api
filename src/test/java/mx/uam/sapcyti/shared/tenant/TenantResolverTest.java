package mx.uam.sapcyti.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TenantResolverTest {

    @Test
    void usesClaimWhenHeaderAbsent() {
        assertThat(TenantResolver.resolve("COORDINATOR", 5L, Optional.empty()))
                .contains(5L);
    }

    @Test
    void acceptsMatchingHeader() {
        assertThat(TenantResolver.resolve("STUDENT", 5L, Optional.of(5L)))
                .contains(5L);
    }

    @Test
    void rejectsMismatchedHeader() {
        assertThatThrownBy(() -> TenantResolver.resolve("PROFESSOR", 5L, Optional.of(9L)))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessage(TenantAccessDeniedException.MISMATCH_MESSAGE);
    }

    @Test
    void rejectsUserWithoutClaimProgram() {
        assertThatThrownBy(() -> TenantResolver.resolve("ASSISTANT", null, Optional.empty()))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessage(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
    }

    @Test
    void systemAdminUsesHeaderOverride() {
        assertThat(TenantResolver.resolve("SYSTEM_ADMIN", null, Optional.of(99L)))
                .contains(99L);
    }

    @Test
    void systemAdminWithoutHeaderHasNoTenant() {
        assertThat(TenantResolver.resolve("SYSTEM_ADMIN", null, Optional.empty()))
                .isEmpty();
    }

    @Test
    void detectsInvalidHeader() {
        assertThat(TenantResolver.isHeaderPresentButInvalid("not-a-number")).isTrue();
        assertThat(TenantResolver.isHeaderPresentButInvalid("  ")).isFalse();
        assertThat(TenantResolver.parseGraduateHeader("42")).contains(42L);
    }
}
