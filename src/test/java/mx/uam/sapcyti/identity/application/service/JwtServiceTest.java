package mx.uam.sapcyti.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import io.jsonwebtoken.Claims;
import java.util.Base64;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();
        
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        
        jwtService.setKeys(pair.getPrivate(), pair.getPublic());
    }

    @Test
    @DisplayName("should generate and validate access token")
    void shouldGenerateAndValidateAccessToken() {
        User user = new User("test@uam.mx", "hash", RoleType.STUDENT, 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 123L);

        String token = jwtService.generateAccessToken(user);
        assertThat(token).isNotBlank();

        Claims claims = jwtService.validateToken(token);
        assertThat(claims.getSubject()).isEqualTo("123");
        assertThat(claims.get("role")).isEqualTo("STUDENT");
        assertThat(claims.get("graduateProgramId", Long.class)).isEqualTo(1L);
    }

    @Test
    @DisplayName("should hash token consistently")
    void shouldHashTokenConsistently() {
        String token = "plain-token";
        String hash1 = jwtService.hashToken(token);
        String hash2 = jwtService.hashToken(token);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 hex is 64 chars
    }

    @Test
    @DisplayName("should initialize keys from PEM strings")
    void shouldInitKeysFromPem() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()) +
                "\n-----END PRIVATE KEY-----";
        String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()) +
                "\n-----END PUBLIC KEY-----";

        JwtService service = new JwtService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "privateKeyPem", privateKeyPem);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "publicKeyPem", publicKeyPem);

        service.init();

        User user = new User("test@uam.mx", "hash", RoleType.STUDENT, 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 123L);

        String token = service.generateAccessToken(user);
        assertThat(token).isNotBlank();
        
        Claims claims = service.validateToken(token);
        assertThat(claims.getSubject()).isEqualTo("123");
    }
}
