package mx.uam.sapcyti.offering.infrastructure.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import mx.uam.sapcyti.configuration.domain.model.GraduateProgram;
import mx.uam.sapcyti.configuration.infrastructure.adapter.out.GraduateProgramJpaAdapter;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.model.User;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
import mx.uam.sapcyti.identity.infrastructure.adapter.out.repository.SpringDataUserRepository;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;
import mx.uam.sapcyti.offering.infrastructure.adapter.in.dto.RegisterUeaRequest;
import mx.uam.sapcyti.offering.infrastructure.adapter.in.dto.UpdateUeaRequest;
import mx.uam.sapcyti.offering.infrastructure.adapter.out.repository.SpringDataUeaRepository;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StreamUtils;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
class UeaControllerIT {

    private static final String COORDINATOR_EMAIL = "uea-coordinator@uam.mx";
    private static final String STUDENT_EMAIL = "uea-student@uam.mx";
    private static final String PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataUeaRepository ueaRepository;

    @Autowired
    private GraduateProgramJpaAdapter programAdapter;

    private Long programId;

    @BeforeEach
    void seed() {
        ueaRepository.deleteAll();
        userRepository.deleteAll();

        GraduateProgram program = programAdapter.save(
                new GraduateProgram("PCyTI UEA " + System.nanoTime(), "CBI"));
        programId = program.getId();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(PASSWORD);
        userRepository.save(new User(COORDINATOR_EMAIL, hash, RoleType.COORDINATOR, programId));
        userRepository.save(new User(STUDENT_EMAIL, hash, RoleType.STUDENT, programId));
    }

    @Test
    @DisplayName("coordinator registers UEA and lists catalog")
    void registerAndList() throws Exception {
        RegisterUeaRequest request = sampleRequest("2156041", "MÉTODOS MATEMÁTICOS", 9);

        mockMvc.perform(post("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clave").value("2156041"))
                .andExpect(jsonPath("$.creditos").value(9))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .param("search", "2156041"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].clave").value("2156041"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("get UEA by id returns the catalog item")
    void getById() throws Exception {
        long ueaId = createUea("2156041", "MÉTODOS MATEMÁTICOS", 9);

        mockMvc.perform(get("/api/ueas/{ueaId}", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) ueaId))
                .andExpect(jsonPath("$.clave").value("2156041"))
                .andExpect(jsonPath("$.nombre").value("MÉTODOS MATEMÁTICOS"));
    }

    @Test
    @DisplayName("get unknown UEA returns NOT_FOUND")
    void getByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/ueas/{ueaId}", 99999L)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("list sorts by nombre desc when requested")
    void listSortsByNombreDesc() throws Exception {
        createUea("2156041", "AAA PRIMERA", 9);
        createUea("2156099", "ZZZ ULTIMA", 9);

        mockMvc.perform(get("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .param("sort", "nombre,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("ZZZ ULTIMA"))
                .andExpect(jsonPath("$.content[1].nombre").value("AAA PRIMERA"));
    }

    @Test
    @DisplayName("coordinator updates UEA editable fields")
    void updateUea() throws Exception {
        RegisterUeaRequest createRequest = sampleRequest("2156041", "MÉTODOS MATEMÁTICOS", 9);
        MvcResult created = mockMvc.perform(post("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long ueaId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        UpdateUeaRequest updateRequest = new UpdateUeaRequest(
                "MÉTODOS MATEMÁTICOS ACTUALIZADOS",
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                new BigDecimal("4.5"),
                new BigDecimal("0"),
                FormationType.BASICA,
                12);

        mockMvc.perform(put("/api/ueas/{ueaId}", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clave").value("2156041"))
                .andExpect(jsonPath("$.nombre").value("MÉTODOS MATEMÁTICOS ACTUALIZADOS"))
                .andExpect(jsonPath("$.creditos").value(12))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("update rejects clave in request body")
    void updateRejectsClave() throws Exception {
        RegisterUeaRequest createRequest = sampleRequest("2156042", "UEA prueba", 9);
        MvcResult created = mockMvc.perform(post("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long ueaId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        String body = """
                {
                  "clave": "9999999",
                  "nombre": "Nombre",
                  "tipo": "OPTATIVA",
                  "modalidad": "MIXTA",
                  "horasTeoria": 3,
                  "horasPractica": 3,
                  "tipoFormacion": "COMPLEMENTARIA",
                  "creditos": 9
                }
                """;

        mockMvc.perform(put("/api/ueas/{ueaId}", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("update unknown UEA returns NOT_FOUND")
    void updateNotFound() throws Exception {
        UpdateUeaRequest updateRequest = new UpdateUeaRequest(
                "Nombre",
                UeaType.OPTATIVA,
                UeaModality.MIXTA,
                new BigDecimal("3"),
                new BigDecimal("3"),
                FormationType.COMPLEMENTARIA,
                9);

        mockMvc.perform(put("/api/ueas/{ueaId}", 99999L)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("coordinator deactivates active UEA")
    void deactivateUea() throws Exception {
        long ueaId = createUea("2156041", "MÉTODOS MATEMÁTICOS", 9);

        mockMvc.perform(put("/api/ueas/{ueaId}/deactivate", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.clave").value("2156041"));

        assertThat(ueaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("deactivated UEA excluded from active list and visible when inactive filter")
    void deactivateFiltersList() throws Exception {
        long ueaId = createUea("2156041", "MÉTODOS MATEMÁTICOS", 9);

        mockMvc.perform(put("/api/ueas/{ueaId}/deactivate", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) ueaId))
                .andExpect(jsonPath("$.content[0].active").value(false));
    }

    @Test
    @DisplayName("double deactivation returns UEA_ALREADY_INACTIVE")
    void deactivateAlreadyInactive() throws Exception {
        long ueaId = createUea("2156042", "UEA prueba", 9);

        mockMvc.perform(put("/api/ueas/{ueaId}/deactivate", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/ueas/{ueaId}/deactivate", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("UEA_ALREADY_INACTIVE"));
    }

    @Test
    @DisplayName("deactivate unknown UEA returns NOT_FOUND")
    void deactivateNotFound() throws Exception {
        mockMvc.perform(put("/api/ueas/{ueaId}/deactivate", 99999L)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("coordinator restores inactive UEA")
    void restoreUea() throws Exception {
        long ueaId = createUea("2156041", "MÉTODOS MATEMÁTICOS", 9);

        mockMvc.perform(put("/api/ueas/{ueaId}/deactivate", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/ueas/{ueaId}/restore", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.clave").value("2156041"))
                .andExpect(jsonPath("$.nombre").value("MÉTODOS MATEMÁTICOS"))
                .andExpect(jsonPath("$.creditos").value(9));

        assertThat(ueaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("restored UEA appears in active catalog list")
    void restoreFiltersList() throws Exception {
        long ueaId = createUea("2156041", "MÉTODOS MATEMÁTICOS", 9);

        mockMvc.perform(put("/api/ueas/{ueaId}/deactivate", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/ueas/{ueaId}/restore", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) ueaId))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    @DisplayName("double restore returns UEA_ALREADY_ACTIVE")
    void restoreAlreadyActive() throws Exception {
        long ueaId = createUea("2156042", "UEA prueba", 9);

        mockMvc.perform(put("/api/ueas/{ueaId}/restore", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("UEA_ALREADY_ACTIVE"));
    }

    @Test
    @DisplayName("restore unknown UEA returns NOT_FOUND")
    void restoreNotFound() throws Exception {
        mockMvc.perform(put("/api/ueas/{ueaId}/restore", 99999L)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("student cannot restore UEAs")
    void studentCannotRestore() throws Exception {
        long ueaId = createUea("2156043", "UEA prueba", 9);

        mockMvc.perform(put("/api/ueas/{ueaId}/deactivate", ueaId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/ueas/{ueaId}/restore", ueaId)
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("duplicate clave returns UEA_ALREADY_EXISTS")
    void duplicateClave() throws Exception {
        RegisterUeaRequest request = sampleRequest("2156041", "Primera UEA", 9);
        register(request);

        mockMvc.perform(post("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("UEA_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("invalid clave format returns CLAVE_INVALID_FORMAT")
    void invalidClaveFormat() throws Exception {
        RegisterUeaRequest request = sampleRequest("ABC-2156041", "Nombre", 9);

        mockMvc.perform(post("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CLAVE_INVALID_FORMAT"));
    }

    @Test
    @DisplayName("student role is forbidden")
    void studentForbidden() throws Exception {
        mockMvc.perform(get("/api/ueas")
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("bulk upload inserts 13 rows from HU-46 sample CSV")
    void bulkUploadSuccess() throws Exception {
        byte[] csv = StreamUtils.copyToByteArray(new ClassPathResource("uea/hu46-catalog-13-rows.csv").getInputStream());
        MockMultipartFile file = new MockMultipartFile("file", "catalog.csv", "text/csv", csv);

        mockMvc.perform(multipart("/api/ueas/bulk")
                        .file(file)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(13))
                .andExpect(jsonPath("$.errors").isEmpty());

        assertThat(ueaRepository.count()).isEqualTo(13);
    }

    @Test
    @DisplayName("bulk upload is all-or-nothing on invalid credits")
    void bulkUploadAllOrNothing() throws Exception {
        String csv = """
                Clave,NOMBRE UEA,TIPO,MODALIDAD,H. TEOR.,H. PRAC.,Tipo formacion,Creditos
                2156024,REDES Y PROTOCOLOS,OBLIGATORIA,MIXTA,3,3,Basica,9
                2156027,INTELIGENCIA ARTIFICIAL,OBLIGATORIA,MIXTA,3,3,Basica,0
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "catalog.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/ueas/bulk")
                        .file(file)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_CREDITS"));

        assertThat(ueaRepository.count()).isZero();
    }

    @Test
    @DisplayName("bulk upload rejects PDF with FILE_FORMAT_INVALID")
    void bulkUploadRejectPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "catalog.pdf", "application/pdf", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/ueas/bulk")
                        .file(file)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("FILE_FORMAT_INVALID"));
    }

    private void register(RegisterUeaRequest request) throws Exception {
        mockMvc.perform(post("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private long createUea(String clave, String nombre, int creditos) throws Exception {
        RegisterUeaRequest request = sampleRequest(clave, nombre, creditos);
        MvcResult created = mockMvc.perform(post("/api/ueas")
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    private static RegisterUeaRequest sampleRequest(String clave, String nombre, int creditos) {
        return new RegisterUeaRequest(
                clave,
                nombre,
                UeaType.OPTATIVA,
                UeaModality.MIXTA,
                new BigDecimal("3"),
                new BigDecimal("3"),
                FormationType.COMPLEMENTARIA,
                creditos);
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
