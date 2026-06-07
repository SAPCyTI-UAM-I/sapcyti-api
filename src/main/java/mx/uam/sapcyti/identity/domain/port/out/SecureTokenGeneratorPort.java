package mx.uam.sapcyti.identity.domain.port.out;

/**
 * Generates cryptographically secure opaque tokens (HU-02, SPEC-015).
 */
public interface SecureTokenGeneratorPort {
    String generateToken();
}
