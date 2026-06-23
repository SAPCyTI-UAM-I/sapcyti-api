package mx.uam.sapcyti.academic.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.identity.domain.port.out.SecureTokenGeneratorPort;
import org.springframework.stereotype.Service;

/**
 * Generates cryptographically secure 12-character passwords (HU-15, HU-21).
 */
@Service
@RequiredArgsConstructor
public class PasswordGenerationService {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%&*";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;
    private static final int PASSWORD_LENGTH = 12;

    private final SecureTokenGeneratorPort tokenGenerator;

    public String generatePassword() {
        SecureRandom random = new SecureRandom(tokenGenerator.generateToken().getBytes(StandardCharsets.UTF_8));
        char[] password = new char[PASSWORD_LENGTH];
        password[0] = pick(random, UPPER);
        password[1] = pick(random, LOWER);
        password[2] = pick(random, DIGITS);
        password[3] = pick(random, SPECIAL);
        for (int i = 4; i < PASSWORD_LENGTH; i++) {
            password[i] = pick(random, ALL);
        }
        shuffle(random, password);
        return new String(password);
    }

    private static char pick(SecureRandom random, String charset) {
        return charset.charAt(random.nextInt(charset.length()));
    }

    private static void shuffle(SecureRandom random, char[] password) {
        for (int i = password.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = password[i];
            password[i] = password[j];
            password[j] = temp;
        }
    }
}
