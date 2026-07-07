package mx.uam.sapcyti.academic.infrastructure.adapter.in;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.internoProfessor;
import static mx.uam.sapcyti.academic.AcademicTestFixtures.minimalProfessorPersonalData;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Professor;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.infrastructure.adapter.in.dto.UpdateStudentProgramRequest;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataProfessorRepository;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataStudentProgramRepository;
import mx.uam.sapcyti.academic.infrastructure.adapter.out.repository.SpringDataStudentRepository;
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
class StudentProgramControllerIT {

    private static final String COORDINATOR_EMAIL = "coordinator-program@uam.mx";
    private static final String STUDENT_ROLE_EMAIL = "student-program@uam.mx";
    private static final String PROFESSOR_ROLE_EMAIL = "professor-program@uam.mx";
    private static final String PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataStudentRepository studentRepository;

    @Autowired
    private SpringDataProfessorRepository professorRepository;

    @Autowired
    private SpringDataStudentProgramRepository studentProgramRepository;

    @Autowired
    private GraduateProgramJpaAdapter programAdapter;

    private Long programId;
    private Long studentId;
    private Long studentProgramId;
    private Long tutorId;
    private Long advisorId;

    @BeforeEach
    void seed() {
        studentProgramRepository.deleteAll();
        studentRepository.deleteAll();
        professorRepository.deleteAll();
        userRepository.deleteAll();

        GraduateProgram program = programAdapter.save(
                new GraduateProgram("PCyTI Program " + System.nanoTime(), "CBI"));
        programId = program.getId();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(PASSWORD);

        User coordinator = userRepository.save(new User(COORDINATOR_EMAIL, hash, RoleType.COORDINATOR, programId));
        userRepository.save(new User(STUDENT_ROLE_EMAIL, hash, RoleType.STUDENT, programId));
        userRepository.save(new User(PROFESSOR_ROLE_EMAIL, hash, RoleType.PROFESSOR, programId));
        User tutorUser = userRepository.save(new User("tutor.program@uam.mx", hash, RoleType.PROFESSOR, programId));
        User advisorUser = userRepository.save(new User("advisor.program@uam.mx", hash, RoleType.PROFESSOR, programId));

        Professor tutor = professorRepository.save(internoProfessor(
                "30568",
                tutorUser.getId(),
                programId,
                minimalProfessorPersonalData("Humberto", "Cervantes")));
        Professor advisor = professorRepository.save(internoProfessor(
                "30569",
                advisorUser.getId(),
                programId,
                minimalProfessorPersonalData("Manuel", "Aguilar")));
        tutorId = tutor.getId();
        advisorId = advisor.getId();

        Student student = studentRepository.save(new Student(
                "2123803361",
                coordinator.getId() + 200,
                programId,
                advisorId,
                new PersonalData("Paulina", "Valencia", "Franco", "Mexicana", LocalDate.of(1998, 3, 15), "5554821234", null),
                new mx.uam.sapcyti.academic.domain.model.AcademicInformation(
                        "Computación", mx.uam.sapcyti.academic.domain.model.DegreeLevel.LICENCIATURA,
                        ProgramType.MAESTRIA, LocalDate.of(2023, 9, 1))));
        studentId = student.getId();

        StudentProgram studentProgram = studentProgramRepository.save(new StudentProgram(
                studentId,
                programId,
                "2123803361",
                ProgramType.MAESTRIA,
                LocalDate.of(2023, 9, 1),
                ProgramStatus.ACTIVO));
        studentProgramId = studentProgram.getId();
    }

    @Test
    @DisplayName("HU-19: coordinator views student program details")
    void getProgramDetails() throws Exception {
        mockMvc.perform(get("/api/students/{studentId}/programs/{programId}", studentId, studentProgramId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentProgramId.intValue()))
                .andExpect(jsonPath("$.enrollmentId").value("2123803361"))
                .andExpect(jsonPath("$.programType").value("MAESTRIA"))
                .andExpect(jsonPath("$.admissionDate").value("2023-09-01"));
    }

    @Test
    @DisplayName("HU-19: coordinator lists student programs")
    void listPrograms() throws Exception {
        mockMvc.perform(get("/api/students/{studentId}/programs", studentId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(studentProgramId.intValue()))
                .andExpect(jsonPath("$[0].programType").value("MAESTRIA"))
                .andExpect(jsonPath("$[0].hasTutor").value(false));
    }

    @Test
    @DisplayName("HU-20: coordinator assigns tutor and advisors")
    void updateProgramAssignments() throws Exception {
        UpdateStudentProgramRequest request = new UpdateStudentProgramRequest(
                LocalDate.of(2023, 9, 1),
                null,
                "Ciencias e Ingeniería de la Computación",
                "Inteligencia artificial",
                ProgramStatus.ACTIVO,
                null,
                tutorId,
                List.of(advisorId));

        mockMvc.perform(put("/api/students/{studentId}/programs/{programId}", studentId, studentProgramId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorId").value(tutorId.intValue()))
                .andExpect(jsonPath("$.tutor.firstName").value("Humberto"))
                .andExpect(jsonPath("$.advisorIds[0]").value(advisorId.intValue()))
                .andExpect(jsonPath("$.researchArea").value("Inteligencia artificial"))
                .andExpect(jsonPath("$.lineOfKnowledge").value("Ciencias e Ingeniería de la Computación"));
    }

    @Test
    @DisplayName("HU-20: rejects non-existent tutor")
    void rejectUnknownTutor() throws Exception {
        UpdateStudentProgramRequest request = sampleUpdateRequest(999L, List.of());

        mockMvc.perform(put("/api/students/{studentId}/programs/{programId}", studentId, studentProgramId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Professor not found"));
    }

    @Test
    @DisplayName("HU-20: rejects duplicate advisors")
    void rejectDuplicateAdvisors() throws Exception {
        UpdateStudentProgramRequest request = sampleUpdateRequest(null, List.of(tutorId, tutorId));

        mockMvc.perform(put("/api/students/{studentId}/programs/{programId}", studentId, studentProgramId)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Duplicate advisor")));
    }

    @Test
    @DisplayName("HU-19: student role cannot view program details")
    void rejectStudentRole() throws Exception {
        mockMvc.perform(get("/api/students/{studentId}/programs/{programId}", studentId, studentProgramId)
                        .header("Authorization", "Bearer " + studentToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HU-20: professor role cannot update program")
    void rejectProfessorRole() throws Exception {
        UpdateStudentProgramRequest request = sampleUpdateRequest(tutorId, List.of());

        mockMvc.perform(put("/api/students/{studentId}/programs/{programId}", studentId, studentProgramId)
                        .header("Authorization", "Bearer " + professorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HU-19: non-existent program returns 404")
    void programNotFound() throws Exception {
        mockMvc.perform(get("/api/students/{studentId}/programs/{programId}", studentId, 999999L)
                        .header("Authorization", "Bearer " + coordinatorToken())
                        .header(TenantFilter.HEADER_GRADUATE_ID, programId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student program not found"));
    }

    private UpdateStudentProgramRequest sampleUpdateRequest(Long tutorId, List<Long> advisorIds) {
        return new UpdateStudentProgramRequest(
                LocalDate.of(2023, 9, 1),
                null,
                null,
                null,
                ProgramStatus.ACTIVO,
                null,
                tutorId,
                advisorIds);
    }

    private String coordinatorToken() throws Exception {
        return login(COORDINATOR_EMAIL);
    }

    private String studentToken() throws Exception {
        return login(STUDENT_ROLE_EMAIL);
    }

    private String professorToken() throws Exception {
        return login(PROFESSOR_ROLE_EMAIL);
    }

    private String login(String email) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(PASSWORD)
                .build();
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                                .header(TenantFilter.HEADER_GRADUATE_ID, programId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }
}
