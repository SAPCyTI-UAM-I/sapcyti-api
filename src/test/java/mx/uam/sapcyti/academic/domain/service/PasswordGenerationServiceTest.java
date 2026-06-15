package mx.uam.sapcyti.academic.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import mx.uam.sapcyti.identity.domain.port.out.SecureTokenGeneratorPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordGenerationServiceTest {

    @Mock
    private SecureTokenGeneratorPort tokenGenerator;

    private PasswordGenerationService service;

    @BeforeEach
    void setUp() {
        service = new PasswordGenerationService(tokenGenerator);
    }

    @RepeatedTest(5)
    @DisplayName("generates 12-char password with mixed complexity")
    void generatesSecurePassword() {
        lenient().when(tokenGenerator.generateToken()).thenReturn("entropy-seed-token-value");

        String password = service.generatePassword();

        assertThat(password).hasSize(12);
        assertThat(password).matches(".*[A-Z].*");
        assertThat(password).matches(".*[a-z].*");
        assertThat(password).matches(".*[0-9].*");
        assertThat(password).matches(".*[!@#$%&*].*");
    }

    @Test
    @DisplayName("generates different passwords across calls")
    void generatesUniquePasswords() {
        when(tokenGenerator.generateToken())
                .thenReturn("seed-one")
                .thenReturn("seed-two");

        String first = service.generatePassword();
        String second = service.generatePassword();

        assertThat(first).isNotEqualTo(second);
    }
}
