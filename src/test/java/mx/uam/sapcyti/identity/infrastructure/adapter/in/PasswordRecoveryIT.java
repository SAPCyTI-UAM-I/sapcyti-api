package mx.uam.sapcyti.identity.infrastructure.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.infrastructure.adapter.out.GraduateProgramJpaAdapter;
import mx.uam.sapcyti.identity.domain.model.PasswordResetToken;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.domain.port.out.EmailPort;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.ForgotPasswordRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.ResetPasswordRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class PasswordRecoveryIT {

    private static final String EMAIL = "alumno@uam.mx";
    private static final String PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private GraduateProgramJpaAdapter programAdapter;

    @MockitoBean
    private EmailPort emailPort;

    @BeforeEach
    void seedUser() {
        userRepository.deleteAll();

        GraduateProgram program = programAdapter.save(new GraduateProgram("PCyTI Recovery " + System.nanoTime(), "CBI"));
        String hash = new BCryptPasswordEncoder().encode(PASSWORD);
        userRepository.save(new User(EMAIL, hash, RoleType.STUDENT, program.getId()));
    }

    @Test
    @DisplayName("forgot-password returns generic 200 for registered email and sends mail")
    void forgotPasswordRegistered() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email(EMAIL).build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "If an account with that email exists, a recovery email has been sent"));

        verify(emailPort).sendPasswordReset(eq(EMAIL), any(), any());
    }

    @Test
    @DisplayName("forgot-password returns same 200 for unregistered email without sending mail")
    void forgotPasswordUnregistered() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("missing@uam.mx").build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "If an account with that email exists, a recovery email has been sent"));
    }

    @Test
    @DisplayName("reset-password succeeds with valid token")
    void resetPasswordSuccess() throws Exception {
        String rawToken = requestResetToken();

        ResetPasswordRequest reset = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("NewS3cur3!Pass")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reset)))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(new BCryptPasswordEncoder().matches("NewS3cur3!Pass", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("reset-password rejects invalid token")
    void resetPasswordInvalidToken() throws Exception {
        ResetPasswordRequest reset = ResetPasswordRequest.builder()
                .token("invalid-token")
                .newPassword("NewS3cur3!Pass")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reset)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("Invalid reset token"));
    }

    @Test
    @DisplayName("reset-password rejects expired token")
    void resetPasswordExpiredToken() throws Exception {
        String rawToken = "expired-token";
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        user.setPasswordResetToken(new PasswordResetToken(
                PasswordResetToken.hashToken(rawToken),
                java.time.Instant.now().minusSeconds(60)));
        userRepository.save(user);

        ResetPasswordRequest reset = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("NewS3cur3!Pass")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reset)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("EXPIRED_TOKEN"));
    }

    @Test
    @DisplayName("reset-password rejects used token")
    void resetPasswordUsedToken() throws Exception {
        String rawToken = requestResetToken();

        ResetPasswordRequest reset = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("NewS3cur3!Pass")
                .build();
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reset)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reset)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TOKEN_USED"));
    }

    @Test
    @DisplayName("new forgot-password invalidates previous token")
    void newForgotInvalidatesPreviousToken() throws Exception {
        String firstToken = requestResetToken();
        requestResetToken();

        ResetPasswordRequest reset = ResetPasswordRequest.builder()
                .token(firstToken)
                .newPassword("NewS3cur3!Pass")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reset)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("forgot-password validates email format")
    void forgotPasswordValidation() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("not_an_email").build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("forgot-password rejects empty email")
    void forgotPasswordEmptyEmail() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("").build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is required"));
    }

    @Test
    @DisplayName("password recovery endpoints are accessible without JWT")
    void publicEndpointsNoJwt() throws Exception {
        ForgotPasswordRequest forgot = ForgotPasswordRequest.builder().email(EMAIL).build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgot)))
                .andExpect(status().isOk());

        ResetPasswordRequest reset = ResetPasswordRequest.builder()
                .token("any")
                .newPassword("short")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reset)))
                .andExpect(status().isBadRequest());
    }

    private String requestResetToken() throws Exception {
        triggerForgotPassword();
        return captureLastSentToken();
    }

    private void triggerForgotPassword() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email(EMAIL).build();
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private String captureLastSentToken() {
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailPort, atLeastOnce()).sendPasswordReset(eq(EMAIL), tokenCaptor.capture(), any());
        return tokenCaptor.getAllValues().getLast();
    }
}
