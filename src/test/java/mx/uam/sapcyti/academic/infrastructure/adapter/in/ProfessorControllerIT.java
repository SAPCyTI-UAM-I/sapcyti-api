package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProfessorInformation;
import mx.uam.sapcyti.academic.domain.model.ProfessorType;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.RegisterProfessorRequest;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.UpdateProfessorRequest;
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
        RegisterProfessorRequest request = sampleRequest();

        MvcResult result = mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/professors/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.employeeNumber").value("30568"))
                .andExpect(jsonPath("$.professorType").value("INTERNO"))
                .andExpect(jsonPath("$.email").value("humberto.cervantes@uam.mx"))
                .andExpect(jsonPath("$.commissionMember").value(true))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.active").value(true))
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
        RegisterProfessorRequest request = sampleRequestBuilder()
                .employeeNumber("30569")
                .email("prof.no-second@uam.mx")
                .firstName("Ana")
                .firstLastName("Lopez")
                .secondLastName(null)
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
    @DisplayName("registration without sabbatical dates succeeds")
    void registerWithoutSabbaticalDates() throws Exception {
        RegisterProfessorRequest request = sampleRequestBuilder()
                .employeeNumber("30570")
                .email("prof.no-sabbatical@uam.mx")
                .commissionMember(false)
                .nextSabbaticalStart(null)
                .nextSabbaticalEnd(null)
                .build();

        mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nextSabbaticalStart").doesNotExist())
                .andExpect(jsonPath("$.nextSabbaticalEnd").doesNotExist());
    }

    @Test
    @DisplayName("registration without phoneExtension succeeds")
    void registerWithoutPhoneExtension() throws Exception {
        RegisterProfessorRequest request = sampleRequestBuilder()
                .employeeNumber("30571")
                .email("prof.no-extension@uam.mx")
                .phoneExtension(null)
                .build();

        mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phoneExtension").doesNotExist());
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
        User existingUser = userRepository.save(new User(
                "existing.prof@uam.mx", "hash", RoleType.PROFESSOR, programId));
        professorRepository.save(new Professor(
                ProfessorType.INTERNO,
                "30568",
                existingUser.getId(),
                programId,
                new PersonalData("Existing", "Professor", null, null, null, "5554820000", null),
                new ProfessorInformation(false, null, null)));

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
    @DisplayName("missing employee number for interno returns 400")
    void missingEmployeeNumberForInterno() throws Exception {
        RegisterProfessorRequest request = sampleRequestBuilder()
                .professorType(ProfessorType.INTERNO)
                .employeeNumber(null)
                .build();

        mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee number is required for internal professors"));
    }

    @Test
    @DisplayName("PUT updates professor fields")
    void updateProfessor() throws Exception {
        Long professorId = createProfessor();

        UpdateProfessorRequest update = UpdateProfessorRequest.builder()
                .professorType(ProfessorType.INTERNO)
                .employeeNumber("30568")
                .email("humberto.nuevo@uam.mx")
                .firstName("Humberto Gustavo")
                .firstLastName("Cervantes")
                .secondLastName("Maceda")
                .phone("5559998877")
                .phoneExtension("1234")
                .commissionMember(false)
                .nextSabbaticalStart(LocalDate.of(2028, 1, 1))
                .nextSabbaticalEnd(LocalDate.of(2028, 6, 30))
                .build();

        mockMvc.perform(put("/api/professors/{id}", professorId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("humberto.nuevo@uam.mx"))
                .andExpect(jsonPath("$.commissionMember").value(false));
    }

    @Test
    @DisplayName("PUT deactivate sets active false")
    void deactivateProfessor() throws Exception {
        Long professorId = createProfessor();

        mockMvc.perform(put("/api/professors/{id}/deactivate", professorId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/professors")
                        .param("active", "true")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("deactivate already inactive professor returns 409")
    void deactivateAlreadyInactive() throws Exception {
        Long professorId = createProfessor();

        mockMvc.perform(put("/api/professors/{id}/deactivate", professorId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/professors/{id}/deactivate", professorId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Professor is already inactive"));
    }

    @Test
    @DisplayName("invalid email returns 400")
    void invalidEmail() throws Exception {
        RegisterProfessorRequest request = sampleRequestBuilder()
                .email("invalid")
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

        RegisterProfessorRequest request = sampleRequestBuilder().build();

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
                .andExpect(jsonPath("$.professorType").value("INTERNO"))
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
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].employeeNumber").value("30568"))
                .andExpect(jsonPath("$.content[0].userId").isNumber())
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.content[0].generatedPassword").doesNotExist());
    }

    private RegisterProfessorRequest sampleRequest() {
        return sampleRequestBuilder().build();
    }

    private RegisterProfessorRequest.RegisterProfessorRequestBuilder sampleRequestBuilder() {
        return RegisterProfessorRequest.builder()
                .professorType(ProfessorType.INTERNO)
                .employeeNumber("30568")
                .email("humberto.cervantes@uam.mx")
                .graduateProgramId(programId)
                .firstName("Humberto Gustavo")
                .firstLastName("Cervantes")
                .secondLastName("Maceda")
                .phone("5554825678")
                .phoneExtension("4321")
                .commissionMember(true)
                .nextSabbaticalStart(LocalDate.of(2027, 1, 15))
                .nextSabbaticalEnd(LocalDate.of(2027, 7, 15));
    }

    private Long createProfessor() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/professors")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
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
