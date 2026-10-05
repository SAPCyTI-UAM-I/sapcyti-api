package mx.uam.sapcyti.identity.infrastructure.adapter.out;

import java.security.SecureRandom;
import java.util.Base64;
import mx.uam.sapcyti.identity.domain.port.out.SecureTokenGeneratorPort;
import org.springframework.stereotype.Component;

@Component
public class SecureTokenGeneratorAdapter implements SecureTokenGeneratorPort {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
