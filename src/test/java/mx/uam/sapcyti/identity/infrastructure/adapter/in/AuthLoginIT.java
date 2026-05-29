package mx.uam.sapcyti.identity.infrastructure.adapter.in;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.infrastructure.adapter.out.GraduateProgramJpaAdapter;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
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
class AuthLoginIT {

    private static final String PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private GraduateProgramJpaAdapter programAdapter;

    @BeforeEach
    void seedUser() {
        userRepository.deleteAll();

        GraduateProgram program = programAdapter.save(new GraduateProgram("PCyTI Test", "CBI"));

        String hash = new BCryptPasswordEncoder().encode(PASSWORD);
        User user = new User("coordinator@uam.mx", hash, RoleType.COORDINATOR, program.getId());
        userRepository.save(user);
    }

    @Test
    @DisplayName("login returns 200 with access token and refresh cookie")
    void loginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("coordinator@uam.mx")
                .password(PASSWORD)
                .rememberMe(false)
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.role").value("COORDINATOR"))
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    @DisplayName("invalid credentials return 401")
    void loginFailure() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("coordinator@uam.mx")
                .password("wrong")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @DisplayName("protected endpoint requires JWT")
    void protectedEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/graduate-programs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("JWT grants access to protected endpoint")
    void protectedEndpointWithJwt() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("coordinator@uam.mx")
                .password(PASSWORD)
                .build();

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken")
                .asText();

        mockMvc.perform(get("/api/graduate-programs")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }
}
