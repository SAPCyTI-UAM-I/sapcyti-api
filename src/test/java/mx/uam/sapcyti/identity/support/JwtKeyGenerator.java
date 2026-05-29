package mx.uam.sapcyti.identity.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * One-off utility to generate dev RSA PEM files. Run via:
 * {@code mvnw -q test-compile exec:java -Dexec.mainClass=mx.uam.sapcyti.identity.support.JwtKeyGenerator -Dexec.classpathScope=test}
 */
public final class JwtKeyGenerator {

    private JwtKeyGenerator() {
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        Path jwtDir = Path.of("src/main/resources/jwt");
        Files.createDirectories(jwtDir);
        Files.writeString(jwtDir.resolve("dev-private.pem"), toPem(pair.getPrivate().getEncoded(), "PRIVATE KEY"));
        Files.writeString(jwtDir.resolve("dev-public.pem"), toPem(pair.getPublic().getEncoded(), "PUBLIC KEY"));
        System.out.println("Wrote " + jwtDir.toAbsolutePath());
    }

    private static String toPem(byte[] der, String label) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n";
    }
}
