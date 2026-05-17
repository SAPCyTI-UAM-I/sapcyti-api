package mx.uam.sapcyti.configuration.infrastructure.adapter.in;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import mx.uam.sapcyti.configuration.application.command.CreateGraduateProgramCommand;
import mx.uam.sapcyti.configuration.application.command.UpdateGraduateProgramCommand;
import mx.uam.sapcyti.configuration.domain.exception.DuplicateGraduateProgramNameException;
import mx.uam.sapcyti.configuration.domain.port.in.CreateGraduateProgramInputPort;
import mx.uam.sapcyti.configuration.domain.port.in.GetGraduateProgramInputPort;
import mx.uam.sapcyti.configuration.domain.port.in.UpdateGraduateProgramInputPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramListItemResponse;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.GraduateProgramResponse;
import mx.uam.sapcyti.configuration.infrastructure.mapper.GraduateProgramMapper;
import mx.uam.sapcyti.shared.config.MethodSecurityConfig;
import mx.uam.sapcyti.shared.web.GlobalExceptionHandler;

@WebMvcTest(GraduateProgramController.class)
@Import({MethodSecurityConfig.class, GlobalExceptionHandler.class})
class GraduateProgramControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateGraduateProgramInputPort createPort;

    @MockitoBean
    private GetGraduateProgramInputPort getPort;

    @MockitoBean
    private UpdateGraduateProgramInputPort updatePort;

    @MockitoBean
    private GraduateProgramMapper mapper;

    @Test
    @DisplayName("Scenario: Successful registration of a graduate program")
    void shouldCreateProgram() throws Exception {
        CreateGraduateProgramCommand command = new CreateGraduateProgramCommand(
            "Ciencias y Tecnologías de la Información", "CBI");
        GraduateProgramResponse response = new GraduateProgramResponse(
            1L, command.name(), command.division(), List.of());

        when(mapper.toCommand(any())).thenReturn(command);
        when(createPort.create(command)).thenReturn(response);

        mockMvc.perform(post("/api/programs")
                .with(user("coordinator").roles("COORDINATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Ciencias y Tecnologías de la Información",
                      "division": "CBI"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name")
                .value("Ciencias y Tecnologías de la Información"));
    }

    @Test
    @DisplayName("Scenario: Rejection of program with empty name")
    void shouldRejectEmptyName() throws Exception {
        mockMvc.perform(post("/api/programs")
                .with(user("coordinator").roles("COORDINATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "", "division": "CBI"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Program name is required"));
    }

    @Test
    @DisplayName("Scenario: Rejection of program with empty division")
    void shouldRejectEmptyDivision() throws Exception {
        mockMvc.perform(post("/api/programs")
                .with(user("coordinator").roles("COORDINATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "PCyTI", "division": ""}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Division is required"));
    }

    @Test
    @DisplayName("Scenario: Rejection of program with duplicate name")
    void shouldReturn409OnDuplicateName() throws Exception {
        CreateGraduateProgramCommand command = new CreateGraduateProgramCommand(
            "Ciencias y Tecnologías de la Información", "CBI");

        when(mapper.toCommand(any())).thenReturn(command);
        when(createPort.create(command))
            .thenThrow(new DuplicateGraduateProgramNameException());

        mockMvc.perform(post("/api/programs")
                .with(user("coordinator").roles("COORDINATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Ciencias y Tecnologías de la Información",
                      "division": "CBI"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message")
                .value("A graduate program with that name already exists"));
    }

    @Test
    @DisplayName("Scenario: Rejection of access with unauthorized role")
    void shouldDenyStudentRole() throws Exception {
        mockMvc.perform(post("/api/programs")
                .with(user("student").roles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "PCyTI", "division": "CBI"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Scenario: Query of non-existent program")
    void shouldReturn404ForMissingProgram() throws Exception {
        when(getPort.getById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/programs/999")
                .with(user("coordinator").roles("COORDINATOR")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Graduate program not found"));
    }

    @Test
    @DisplayName("Scenario: Query all graduate programs")
    void shouldListPrograms() throws Exception {
        when(getPort.listAll()).thenReturn(List.of(
            new GraduateProgramListItemResponse(1L, "PCyTI", "CBI", 2L)));

        mockMvc.perform(get("/api/programs")
                .with(user("coordinator").roles("COORDINATOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].parameterCount").value(2));
    }

    @Test
    @DisplayName("Scenario: Update basic data of a program")
    void shouldUpdateProgram() throws Exception {
        UpdateGraduateProgramCommand command = new UpdateGraduateProgramCommand(
            1L, "Posgrado en Ciencias y Tecnologías de la Información", "CBI");
        GraduateProgramResponse response = new GraduateProgramResponse(
            1L, command.name(), command.division(), List.of());

        when(mapper.toCommand(eq(1L), any())).thenReturn(command);
        when(updatePort.update(command)).thenReturn(response);

        mockMvc.perform(put("/api/programs/1")
                .with(user("coordinator").roles("COORDINATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Posgrado en Ciencias y Tecnologías de la Información",
                      "division": "CBI"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name")
                .value("Posgrado en Ciencias y Tecnologías de la Información"));
    }
}
