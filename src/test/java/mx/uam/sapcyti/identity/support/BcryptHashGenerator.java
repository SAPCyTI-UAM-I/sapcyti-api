package mx.uam.sapcyti.identity.support;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Prints a BCrypt hash for the seed password. */
public final class BcryptHashGenerator {

    private BcryptHashGenerator() {
    }

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = args.length > 0 ? args[0] : "password";
        String existing = "$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2";
        System.out.println("encode=" + encoder.encode(password));
        System.out.println("matches_existing=" + encoder.matches(password, existing));
    }
}
