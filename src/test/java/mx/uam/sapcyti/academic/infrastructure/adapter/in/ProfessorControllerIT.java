package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.RegisterProfessorRequest;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataProfessorRepository;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.infrastructure.adapter.out.GraduateProgramJpaAdapter;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataUserRepository;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
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
class ProfessorControllerIT {

    private static final String COORDINATOR_EMAIL = "coordinator@uam.mx";
    private static final String STUDENT_EMAIL = "student@uam.mx";
    private static final String PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataProfessorRepository professorRepository;

    @Autowired
    private GraduateProgramJpaAdapter programAdapter;

    private Long programId;

    @BeforeEach
    void seed() {
        professorRepository.deleteAll();
        userRepository.deleteAll();

        GraduateProgram program = programAdapter.save(
                new GraduateProgram("PCyTI Prof " + System.nanoTime(), "CBI"));
        programId = program.getId();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(PASSWORD);

        userRepository.save(new User(COORDINATOR_EMAIL, hash, RoleType.COORDINATOR, programId));
        userRepository.save(new User(STUDENT_EMAIL, hash, RoleType.STUDENT, programId));
    }

    @Test
    @DisplayName("coordinator registers professor and receives generated password")
    void registerSuccess() throws Exception {
        RegisterProfessorRequest request = RegisterProfessorRequest.builder()
                .employeeNumber("30568")
                .email("humberto.cervantes@uam.mx")
                .graduateProgramId(programId)
                .firstName("Humberto Gustavo")
                .firstLastName("Cervantes")
                .secondLastName("Maceda")
                .build();

        MvcResult result = mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/professors/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.employeeNumber").value("30568"))
                .andExpect(jsonPath("$.email").value("humberto.cervantes@uam.mx"))
                .andExpect(jsonPath("$.generatedPassword").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String generatedPassword = body.get("generatedPassword").asText();

        User user = userRepository.findByEmail("humberto.cervantes@uam.mx").orElseThrow();
        assertThat(new BCryptPasswordEncoder().matches(generatedPassword, user.getPasswordHash())).isTrue();
        assertThat(user.getRole()).isEqualTo(RoleType.PROFESSOR);
        assertThat(professorRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("registration without secondLastName succeeds")
    void registerWithoutSecondLastName() throws Exception {
        RegisterProfessorRequest request = RegisterProfessorRequest.builder()
                .employeeNumber("30569")
                .email("prof.no-second@uam.mx")
                .graduateProgramId(programId)
                .firstName("Ana")
                .firstLastName("Lopez")
                .build();

        mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.secondLastName").doesNotExist());
    }

    @Test
    @DisplayName("duplicate email returns 409")
    void duplicateEmail() throws Exception {
        userRepository.save(new User(
                "humberto.cervantes@uam.mx", "hash", RoleType.PROFESSOR, programId));

        RegisterProfessorRequest request = sampleRequest();

        mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this email already exists"));
    }

    @Test
    @DisplayName("duplicate employee number returns 409")
    void duplicateEmployeeNumber() throws Exception {
        professorRepository.save(new Professor(
                "30568",
                999L,
                programId,
                new PersonalData("Existing", "Professor", null, null)));

        RegisterProfessorRequest request = sampleRequest();

        mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "A professor with this employee number already exists"));
    }

    @Test
    @DisplayName("missing employee number returns 400")
    void missingRequiredField() throws Exception {
        RegisterProfessorRequest request = RegisterProfessorRequest.builder()
                .employeeNumber("")
                .email("humberto.cervantes@uam.mx")
                .graduateProgramId(programId)
                .firstName("Humberto Gustavo")
                .firstLastName("Cervantes")
                .build();

        mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("invalid email returns 400")
    void invalidEmail() throws Exception {
        RegisterProfessorRequest request = RegisterProfessorRequest.builder()
                .employeeNumber("30568")
                .email("invalid")
                .graduateProgramId(programId)
                .firstName("Humberto Gustavo")
                .firstLastName("Cervantes")
                .build();

        mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email must be valid"));
    }

    @Test
    @DisplayName("unknown graduate program returns 404")
    void unknownProgram() throws Exception {
        programAdapter.deleteById(programId);

        RegisterProfessorRequest request = RegisterProfessorRequest.builder()
                .employeeNumber("30568")
                .email("humberto.cervantes@uam.mx")
                .graduateProgramId(programId)
                .firstName("Humberto Gustavo")
                .firstLastName("Cervantes")
                .build();

        mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Graduate program not found"));
    }

    @Test
    @DisplayName("student role cannot register professor")
    void unauthorizedRole() throws Exception {
        mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET by id returns professor without generatedPassword")
    void getById() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Long professorId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(get("/api/professors/{id}", professorId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeNumber").value("30568"))
                .andExpect(jsonPath("$.generatedPassword").doesNotExist());
    }

    @Test
    @DisplayName("GET unknown professor returns 404")
    void getByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/professors/{id}", 999_999L)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Professor not found"));
    }

    @Test
    @DisplayName("GET list does not expose generatedPassword")
    void listWithoutPassword() throws Exception {
        RegisterProfessorRequest request = sampleRequest();
        mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeNumber").value("30568"))
                .andExpect(jsonPath("$[0].generatedPassword").doesNotExist());
    }

    private RegisterProfessorRequest sampleRequest() {
        return RegisterProfessorRequest.builder()
                .employeeNumber("30568")
                .email("humberto.cervantes@uam.mx")
                .graduateProgramId(programId)
                .firstName("Humberto Gustavo")
                .firstLastName("Cervantes")
                .secondLastName("Maceda")
                .build();
    }

    private String coordinatorToken() throws Exception {
        return login(COORDINATOR_EMAIL);
    }

    private String studentToken() throws Exception {
        return login(STUDENT_EMAIL);
    }

    private String login(String email) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(PASSWORD)
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
