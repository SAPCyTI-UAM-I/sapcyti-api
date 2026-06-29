package mx.uam.sapcyti.offering.infrastructure.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import mx.uam.sapcyti.offering.infrastructure.adapter.in.dto.RegisterUeaRequest;
import mx.uam.sapcyti.offering.infrastructure.adapter.out.repository.SpringDataUeaRepository;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;
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
