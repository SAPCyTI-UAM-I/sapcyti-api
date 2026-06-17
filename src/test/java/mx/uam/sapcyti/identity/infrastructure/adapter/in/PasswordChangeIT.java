package mx.uam.sapcyti.identity.infrastructure.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.infrastructure.adapter.out.GraduateProgramJpaAdapter;
import mx.uam.sapcyti.identity.domain.model.RefreshToken;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.ChangePasswordRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataRefreshTokenRepository;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class PasswordChangeIT {

    private static final String STUDENT_EMAIL = "alumno@uam.mx";
    private static final String COORDINATOR_EMAIL = "coordinator@uam.mx";
    private static final String OTHER_STUDENT_EMAIL = "other@uam.mx";
    private static final String PASSWORD = "OldP@ssword1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private GraduateProgramJpaAdapter programAdapter;

    private Long programId;
    private Long studentId;
    private Long otherStudentId;

    @BeforeEach
    void seedUsers() {
        userRepository.deleteAll();
        refreshTokenRepository.deleteAll();

        GraduateProgram program = programAdapter.save(
                new GraduateProgram("PCyTI Change " + System.nanoTime(), "CBI"));
        programId = program.getId();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(PASSWORD);

        User student = userRepository.save(new User(STUDENT_EMAIL, hash, RoleType.STUDENT, programId));
        studentId = student.getId();

        userRepository.save(new User(COORDINATOR_EMAIL, hash, RoleType.COORDINATOR, programId));

        User otherStudent = userRepository.save(
                new User(OTHER_STUDENT_EMAIL, hash, RoleType.STUDENT, programId));
        otherStudentId = otherStudent.getId();

        refreshTokenRepository.save(new RefreshToken(
                "student-refresh-hash",
                Instant.now().plus(1, ChronoUnit.DAYS),
                "test-device",
                student));
    }

    @Test
    @DisplayName("self-change password succeeds and revokes refresh tokens")
    void selfChangeSuccess() throws Exception {
        String token = login(STUDENT_EMAIL, PASSWORD);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(PASSWORD)
                .newPassword("NewS3cur3!Pass")
                .build();

        mockMvc.perform(put("/api/users/{id}/password", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User updated = userRepository.findById(studentId).orElseThrow();
        assertThat(new BCryptPasswordEncoder().matches("NewS3cur3!Pass", updated.getPasswordHash()))
                .isTrue();
        assertThat(refreshTokenRepository.findAll())
                .allMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("coordinator changes another user's password without current password")
    void coordinatorChangeSuccess() throws Exception {
        String token = login(COORDINATOR_EMAIL, PASSWORD);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .newPassword("TempP@ss2026!")
                .build();

        mockMvc.perform(put("/api/users/{id}/password", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User updated = userRepository.findById(studentId).orElseThrow();
        assertThat(new BCryptPasswordEncoder().matches("TempP@ss2026!", updated.getPasswordHash()))
                .isTrue();
    }

    @Test
    @DisplayName("wrong current password returns 400")
    void wrongCurrentPassword() throws Exception {
        String token = login(STUDENT_EMAIL, PASSWORD);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongOldPass")
                .newPassword("NewS3cur3!Pass")
                .build();

        mockMvc.perform(put("/api/users/{id}/password", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }

    @Test
    @DisplayName("student cannot change another user's password")
    void studentCannotChangeOtherUser() throws Exception {
        String token = login(STUDENT_EMAIL, PASSWORD);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(PASSWORD)
                .newPassword("NewS3cur3!Pass")
                .build();

        mockMvc.perform(put("/api/users/{id}/password", otherStudentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only change your own password"));
    }

    @Test
    @DisplayName("empty new password returns validation error")
    void emptyNewPassword() throws Exception {
        String token = login(STUDENT_EMAIL, PASSWORD);

        String body = """
                {
                  "currentPassword": "%s",
                  "newPassword": null
                }
                """.formatted(PASSWORD);

        mockMvc.perform(put("/api/users/{id}/password", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password is required"));
    }

    @Test
    @DisplayName("weak new password returns validation error")
    void weakNewPassword() throws Exception {
        String token = login(STUDENT_EMAIL, PASSWORD);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(PASSWORD)
                .newPassword("123")
                .build();

        mockMvc.perform(put("/api/users/{id}/password", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters"));
    }

    @Test
    @DisplayName("unauthenticated request returns 401")
    void unauthenticated() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(PASSWORD)
                .newPassword("NewS3cur3!Pass")
                .build();

        mockMvc.perform(put("/api/users/{id}/password", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("unknown user returns 404")
    void userNotFound() throws Exception {
        String token = login(STUDENT_EMAIL, PASSWORD);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(PASSWORD)
                .newPassword("NewS3cur3!Pass")
                .build();

        mockMvc.perform(put("/api/users/{id}/password", 999_999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    private String login(String email, String password) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .rememberMe(false)
                .build();

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }
}
